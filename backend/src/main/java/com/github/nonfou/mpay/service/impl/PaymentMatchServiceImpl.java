package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.dto.monitor.PaymentRecordDTO;
import com.github.nonfou.mpay.service.PaymentMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PaymentMatchServiceImpl implements PaymentMatchService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMatchServiceImpl.class);
    private final WebClient webClient;
    private final StringRedisTemplate redisTemplate;

    public PaymentMatchServiceImpl(WebClient.Builder builder,
            StringRedisTemplate redisTemplate) {
        this.webClient = builder.baseUrl("http://localhost:8100").build();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void handlePaymentRecord(PaymentRecordDTO record) {
        log.info("收到支付记录: {}", record);
        if (record.getPlatformOrder() != null &&
                Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey(record.getPlatformOrder())))) {
            log.info("跳过重复记录: platformOrder={}", record.getPlatformOrder());
            return;
        }
        boolean success = notifyPaymentService(record);
        if (!success) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "支付匹配失败");
        }
        if (record.getPlatformOrder() != null) {
            redisTemplate.opsForValue().set(dedupKey(record.getPlatformOrder()), "1");
        }
    }

    private boolean notifyPaymentService(PaymentRecordDTO record) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("pid", record.getPid());
        payload.put("aid", record.getAid());
        payload.put("payway", record.getPayway());
        payload.put("channel", record.getChannel());
        payload.put("price", record.getPrice());
        payload.put("platformOrder", record.getPlatformOrder());
        try {
            webClient.post()
                .uri("/api/internal/orders/match")
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response -> {
                    log.error("支付匹配响应异常: status={}", response.statusCode());
                    return response.createException();
                })
                .bodyToMono(Void.class)
                .block();
            return true;
        } catch (Exception e) {
            log.error("支付匹配失败: pid={}, price={}", record.getPid(), record.getPrice(), e);
            return false;
        }
    }

    private String dedupKey(String platformOrder) {
        return "mpay:record:" + platformOrder;
    }
}
