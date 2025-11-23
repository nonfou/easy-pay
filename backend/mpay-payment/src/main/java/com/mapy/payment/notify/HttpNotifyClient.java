package com.mapy.payment.notify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapy.payment.entity.OrderEntity;
import com.mapy.payment.service.MerchantSecretService;
import com.mapy.payment.service.NotifyLogService;
import com.mapy.payment.signature.SignatureUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
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
    public void notifyMerchant(OrderEntity order) {
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
        String errorMessage = "notify merchant failed after retries";
        log.error("{} order {}", errorMessage, order.getOrderId());
        notifyLogService.recordFailure(order, errorMessage, maxRetry);
    }

    private boolean send(OrderEntity order, Map<String, Object> payload) {
        try {
            RequestEntity<String> request = RequestEntity.post(new URI(order.getNotifyUrl()))
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(payload));
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);
            log.info("notify {} result {}", order.getNotifyUrl(), response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (URISyntaxException | JsonProcessingException e) {
            log.error("notify merchant error", e);
            return false;
        }
    }
}
