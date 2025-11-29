package com.github.nonfou.mpay.payment.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.nonfou.mpay.payment.dto.alipay.*;
import com.github.nonfou.mpay.payment.properties.AlipayProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(AlipayClient.class)
public class AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    /**
     * PC 端支付固定值
     */
    private static final String FAST_INSTANT_TRADE_PAY = "FAST_INSTANT_TRADE_PAY";

    /**
     * H5 支付固定值
     */
    private static final String QUICK_WAP_WAY = "QUICK_WAP_WAY";

    /**
     * 生成支付二维码
     */
    public AlipayQrcodeResponse createQrcode(AlipayQrcodeRequest request) throws AlipayApiException {
        AlipayTradePrecreateRequest apiRequest = new AlipayTradePrecreateRequest();
        apiRequest.setNotifyUrl(alipayProperties.getNotifyUrl());

        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", request.getOutTradeNo());
        bizContent.put("total_amount", request.getTotalAmount().toString());
        bizContent.put("subject", request.getSubject());
        bizContent.put("timeout_express", request.getTimeoutExpress());

        apiRequest.setBizContent(toJson(bizContent));
        log.debug("支付宝二维码请求: {}", apiRequest.getBizContent());

        AlipayTradePrecreateResponse response = alipayClient.execute(apiRequest);
        log.debug("支付宝二维码响应: {}", response.getBody());

        AlipayQrcodeResponse result = new AlipayQrcodeResponse();
        result.setCode(response.getCode());
        result.setMsg(response.getMsg());
        result.setOutTradeNo(response.getOutTradeNo());
        result.setQrCode(response.getQrCode());
        result.setSubCode(response.getSubCode());
        result.setSubMsg(response.getSubMsg());

        return result;
    }

    /**
     * PC 网站支付 (返回 HTML 表单，自动提交跳转到支付宝)
     */
    public String createPcPay(AlipayPcPayRequest request) throws AlipayApiException {
        AlipayTradePagePayRequest apiRequest = new AlipayTradePagePayRequest();
        apiRequest.setReturnUrl(alipayProperties.getReturnUrl());
        apiRequest.setNotifyUrl(alipayProperties.getNotifyUrl());

        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", request.getOutTradeNo());
        bizContent.put("total_amount", request.getTotalAmount().toString());
        bizContent.put("subject", request.getSubject());
        bizContent.put("product_code", FAST_INSTANT_TRADE_PAY);
        bizContent.put("timeout_express", request.getTimeoutExpress());
        if (request.getBody() != null) {
            bizContent.put("body", request.getBody());
        }

        apiRequest.setBizContent(toJson(bizContent));
        log.debug("支付宝PC支付请求: {}", apiRequest.getBizContent());

        AlipayTradePagePayResponse response = alipayClient.pageExecute(apiRequest);
        log.debug("支付宝PC支付响应: {}", response.getBody());

        return response.getBody();
    }

    /**
     * H5 手机网站支付 (返回 HTML 表单)
     */
    public String createH5Pay(AlipayH5PayRequest request) throws AlipayApiException {
        AlipayTradeWapPayRequest apiRequest = new AlipayTradeWapPayRequest();
        apiRequest.setReturnUrl(alipayProperties.getReturnUrl());
        apiRequest.setNotifyUrl(alipayProperties.getNotifyUrl());

        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", request.getOutTradeNo());
        bizContent.put("total_amount", request.getTotalAmount().toString());
        bizContent.put("subject", request.getSubject());
        bizContent.put("product_code", QUICK_WAP_WAY);
        bizContent.put("timeout_express", request.getTimeoutExpress());
        if (request.getBody() != null) {
            bizContent.put("body", request.getBody());
        }
        if (request.getQuitUrl() != null) {
            bizContent.put("quit_url", request.getQuitUrl());
        }

        apiRequest.setBizContent(toJson(bizContent));
        log.debug("支付宝H5支付请求: {}", apiRequest.getBizContent());

        AlipayTradeWapPayResponse response = alipayClient.pageExecute(apiRequest);
        log.debug("支付宝H5支付响应: {}", response.getBody());

        return response.getBody();
    }

    /**
     * 退款
     */
    public AlipayRefundResponse refund(AlipayRefundRequest request) throws AlipayApiException {
        AlipayTradeRefundRequest apiRequest = new AlipayTradeRefundRequest();

        Map<String, Object> bizContent = new HashMap<>();
        if (request.getOutTradeNo() != null) {
            bizContent.put("out_trade_no", request.getOutTradeNo());
        }
        if (request.getTradeNo() != null) {
            bizContent.put("trade_no", request.getTradeNo());
        }
        bizContent.put("refund_amount", request.getRefundAmount().toString());
        if (request.getRefundReason() != null) {
            bizContent.put("refund_reason", request.getRefundReason());
        }
        if (request.getOutRequestNo() != null) {
            bizContent.put("out_request_no", request.getOutRequestNo());
        }

        apiRequest.setBizContent(toJson(bizContent));
        log.debug("支付宝退款请求: {}", apiRequest.getBizContent());

        AlipayTradeRefundResponse response = alipayClient.execute(apiRequest);
        log.debug("支付宝退款响应: {}", response.getBody());

        AlipayRefundResponse result = new AlipayRefundResponse();
        result.setCode(response.getCode());
        result.setMsg(response.getMsg());
        result.setTradeNo(response.getTradeNo());
        result.setOutTradeNo(response.getOutTradeNo());
        result.setBuyerUserId(response.getBuyerUserId());
        result.setRefundFee(response.getRefundFee() != null ?
                new java.math.BigDecimal(response.getRefundFee()) : null);
        result.setSubCode(response.getSubCode());
        result.setSubMsg(response.getSubMsg());

        return result;
    }

    /**
     * 查询订单
     */
    public AlipayQueryResponse queryOrder(AlipayQueryRequest request) throws AlipayApiException {
        AlipayTradeQueryRequest apiRequest = new AlipayTradeQueryRequest();

        Map<String, Object> bizContent = new HashMap<>();
        if (request.getOutTradeNo() != null) {
            bizContent.put("out_trade_no", request.getOutTradeNo());
        }
        if (request.getTradeNo() != null) {
            bizContent.put("trade_no", request.getTradeNo());
        }

        apiRequest.setBizContent(toJson(bizContent));
        log.debug("支付宝订单查询请求: {}", apiRequest.getBizContent());

        AlipayTradeQueryResponse response = alipayClient.execute(apiRequest);
        log.debug("支付宝订单查询响应: {}", response.getBody());

        AlipayQueryResponse result = new AlipayQueryResponse();
        result.setCode(response.getCode());
        result.setMsg(response.getMsg());
        result.setOutTradeNo(response.getOutTradeNo());
        result.setTradeNo(response.getTradeNo());
        result.setTradeStatus(response.getTradeStatus());
        result.setBuyerUserId(response.getBuyerUserId());
        result.setBuyerLogonId(response.getBuyerLogonId());
        result.setSubCode(response.getSubCode());
        result.setSubMsg(response.getSubMsg());

        if (response.getTotalAmount() != null) {
            result.setTotalAmount(new java.math.BigDecimal(response.getTotalAmount()));
        }
        if (response.getReceiptAmount() != null) {
            result.setReceiptAmount(new java.math.BigDecimal(response.getReceiptAmount()));
        }

        return result;
    }

    /**
     * 关闭订单
     */
    public boolean closeOrder(String outTradeNo) throws AlipayApiException {
        AlipayTradeCloseRequest apiRequest = new AlipayTradeCloseRequest();

        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", outTradeNo);

        apiRequest.setBizContent(toJson(bizContent));
        log.debug("支付宝关闭订单请求: {}", apiRequest.getBizContent());

        AlipayTradeCloseResponse response = alipayClient.execute(apiRequest);
        log.debug("支付宝关闭订单响应: {}", response.getBody());

        return response.isSuccess();
    }

    /**
     * 验证并解析回调参数
     */
    public AlipayCallbackDTO verifyCallback(HttpServletRequest request) throws AlipayApiException {
        Map<String, String> params = getRequestParams(request);

        // 验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                alipayProperties.getPublicKey(),
                alipayProperties.getCharset(),
                alipayProperties.getSignType()
        );

        if (!signVerified) {
            log.error("支付宝回调签名验证失败: {}", params);
            throw new AlipayApiException("签名验证失败");
        }

        log.info("支付宝回调参数: {}", params);

        // 解析回调参数
        AlipayCallbackDTO callback = new AlipayCallbackDTO();
        callback.setOutTradeNo(params.get("out_trade_no"));
        callback.setTradeNo(params.get("trade_no"));
        callback.setTradeStatus(params.get("trade_status"));
        callback.setAppId(params.get("app_id"));
        callback.setBuyerId(params.get("buyer_id"));
        callback.setBuyerLogonId(params.get("buyer_logon_id"));
        callback.setSellerEmail(params.get("seller_email"));
        callback.setSubject(params.get("subject"));

        String totalAmount = params.get("total_amount");
        if (totalAmount != null) {
            callback.setTotalAmount(new java.math.BigDecimal(totalAmount));
        }

        String receiptAmount = params.get("receipt_amount");
        if (receiptAmount != null) {
            callback.setReceiptAmount(new java.math.BigDecimal(receiptAmount));
        }

        return callback;
    }

    /**
     * 获取请求参数 Map
     */
    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }

        return params;
    }

    /**
     * 转换为 JSON 字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }
}
