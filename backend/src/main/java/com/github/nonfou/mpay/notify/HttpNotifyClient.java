package com.github.nonfou.mpay.notify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.service.MerchantSecretService;
import com.github.nonfou.mpay.service.NotifyLogService;
import com.github.nonfou.mpay.signature.SignatureUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpNotifyClient implements NotifyClient {

    private static final Logger log = LoggerFactory.getLogger(HttpNotifyClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MerchantSecretService merchantSecretService;
    private final NotifyLogService notifyLogService;

    public HttpNotifyClient(ObjectMapper objectMapper,
            MerchantSecretService merchantSecretService,
            NotifyLogService notifyLogService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.merchantSecretService = merchantSecretService;
        this.notifyLogService = notifyLogService;
    }

    @Override
    @Async
    public void notifyMerchant(OrderEntity order) {
        Map<String, Object> payload = buildPayload(order);
        int maxRetry = 3;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            if (send(order, payload)) {
                return;
            }
            try {
                Thread.sleep(500L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        String errorMessage = "通知商户失败，已达最大重试次数";
        log.error("{}: orderId={}", errorMessage, order.getOrderId());
        notifyLogService.recordFailure(order, errorMessage, maxRetry);
    }

    @Override
    public boolean sendNotification(OrderEntity order) {
        Map<String, Object> payload = buildPayload(order);
        return send(order, payload);
    }

    private Map<String, Object> buildPayload(OrderEntity order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trade_no", order.getOrderId());
        payload.put("out_trade_no", order.getOutTradeNo());
        payload.put("type", order.getType());
        payload.put("money", order.getMoney());
        payload.put("really_price", order.getReallyPrice());
        payload.put("trade_status", "TRADE_SUCCESS");
        merchantSecretService.getSecret(order.getPid()).ifPresent(secret -> {
            String sign = SignatureUtils.md5(SignatureUtils.buildSignString(payload) + secret);
            payload.put("sign", sign);
            payload.put("sign_type", "MD5");
        });
        return payload;
    }

    private boolean send(OrderEntity order, Map<String, Object> payload) {
        try {
            RequestEntity<String> request = RequestEntity.post(new URI(order.getNotifyUrl()))
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(payload));
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);
            log.info("通知商户: url={}, status={}", order.getNotifyUrl(), response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (URISyntaxException | JsonProcessingException e) {
            log.error("通知商户构造请求失败: orderId={}", order.getOrderId(), e);
            return false;
        } catch (Exception e) {
            log.error("通知商户请求异常: orderId={}, error={}", order.getOrderId(), e.getMessage());
            return false;
        }
    }
}
