package com.github.nonfou.mpay.payment.service;

import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.nonfou.mpay.payment.dto.wxpay.*;
import com.github.nonfou.mpay.payment.properties.WxPayProperties;
import com.github.nonfou.mpay.payment.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 微信支付服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(WxPayService.class)
public class WxPayServiceWrapper {

    private final WxPayService wxPayService;
    private final WxPayProperties wxPayProperties;

    /**
     * 元转分
     */
    private int yuanToFen(BigDecimal yuan) {
        return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 分转元
     */
    private BigDecimal fenToYuan(Integer fen) {
        if (fen == null) {
            return null;
        }
        return new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 二维码支付 (NATIVE)
     */
    public WxPayUnifiedOrderResponse createQrcode(com.github.nonfou.mpay.payment.dto.wxpay.WxPayUnifiedOrderRequest request, HttpServletRequest httpRequest) throws WxPayException {
        WxPayUnifiedOrderRequest wxRequest = new WxPayUnifiedOrderRequest();
        wxRequest.setBody(request.getBody());
        wxRequest.setOutTradeNo(request.getOutTradeNo());
        wxRequest.setTotalFee(yuanToFen(request.getTotalFee()));
        wxRequest.setSpbillCreateIp(request.getSpbillCreateIp() != null ?
                request.getSpbillCreateIp() : IpUtils.getIpAddress(httpRequest));
        wxRequest.setTradeType("NATIVE");
        wxRequest.setNotifyUrl(wxPayProperties.getPayNotifyUrl());

        log.debug("微信二维码支付请求: {}", wxRequest);

        WxPayUnifiedOrderResult result = wxPayService.unifiedOrder(wxRequest);
        log.debug("微信二维码支付响应: {}", result);

        WxPayUnifiedOrderResponse response = new WxPayUnifiedOrderResponse();
        response.setReturnCode(result.getReturnCode());
        response.setReturnMsg(result.getReturnMsg());
        response.setResultCode(result.getResultCode());
        response.setErrCode(result.getErrCode());
        response.setErrCodeDes(result.getErrCodeDes());
        response.setTradeType(result.getTradeType());
        response.setPrepayId(result.getPrepayId());
        response.setCodeUrl(result.getCodeURL());

        return response;
    }

    /**
     * H5 支付 (MWEB)
     */
    public WxPayUnifiedOrderResponse createH5Pay(com.github.nonfou.mpay.payment.dto.wxpay.WxPayUnifiedOrderRequest request, HttpServletRequest httpRequest) throws WxPayException {
        WxPayUnifiedOrderRequest wxRequest = new WxPayUnifiedOrderRequest();
        wxRequest.setBody(request.getBody());
        wxRequest.setOutTradeNo(request.getOutTradeNo());
        wxRequest.setTotalFee(yuanToFen(request.getTotalFee()));
        wxRequest.setSpbillCreateIp(request.getSpbillCreateIp() != null ?
                request.getSpbillCreateIp() : IpUtils.getIpAddress(httpRequest));
        wxRequest.setTradeType("MWEB");
        wxRequest.setNotifyUrl(wxPayProperties.getPayNotifyUrl());

        // H5 支付需要设置场景信息
        if (request.getSceneInfo() != null) {
            wxRequest.setSceneInfo(request.getSceneInfo());
        }

        log.debug("微信H5支付请求: {}", wxRequest);

        WxPayUnifiedOrderResult result = wxPayService.unifiedOrder(wxRequest);
        log.debug("微信H5支付响应: {}", result);

        WxPayUnifiedOrderResponse response = new WxPayUnifiedOrderResponse();
        response.setReturnCode(result.getReturnCode());
        response.setReturnMsg(result.getReturnMsg());
        response.setResultCode(result.getResultCode());
        response.setErrCode(result.getErrCode());
        response.setErrCodeDes(result.getErrCodeDes());
        response.setTradeType(result.getTradeType());
        response.setPrepayId(result.getPrepayId());
        response.setMwebUrl(result.getMwebUrl());

        return response;
    }

    /**
     * 退款
     */
    public WxPayRefundResponse refund(com.github.nonfou.mpay.payment.dto.wxpay.WxPayRefundRequest request) throws WxPayException {
        WxPayRefundRequest wxRequest = new WxPayRefundRequest();
        wxRequest.setOutTradeNo(request.getOutTradeNo());
        wxRequest.setTransactionId(request.getTransactionId());
        wxRequest.setOutRefundNo(request.getOutRefundNo());
        wxRequest.setTotalFee(yuanToFen(request.getTotalFee()));
        wxRequest.setRefundFee(yuanToFen(request.getRefundFee()));
        wxRequest.setRefundDesc(request.getRefundDesc());
        wxRequest.setNotifyUrl(wxPayProperties.getRefundNotifyUrl());

        log.debug("微信退款请求: {}", wxRequest);

        WxPayRefundResult result = wxPayService.refund(wxRequest);
        log.debug("微信退款响应: {}", result);

        WxPayRefundResponse response = new WxPayRefundResponse();
        response.setReturnCode(result.getReturnCode());
        response.setReturnMsg(result.getReturnMsg());
        response.setResultCode(result.getResultCode());
        response.setErrCode(result.getErrCode());
        response.setErrCodeDes(result.getErrCodeDes());
        response.setTransactionId(result.getTransactionId());
        response.setOutTradeNo(result.getOutTradeNo());
        response.setOutRefundNo(result.getOutRefundNo());
        response.setRefundId(result.getRefundId());
        response.setRefundFee(fenToYuan(result.getRefundFee()));
        response.setTotalFee(fenToYuan(result.getTotalFee()));

        return response;
    }

    /**
     * 关闭订单
     */
    public boolean closeOrder(String outTradeNo) throws WxPayException {
        try {
            wxPayService.closeOrder(outTradeNo);
            return true;
        } catch (WxPayException e) {
            log.error("微信关闭订单失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析支付回调
     */
    public WxPayCallbackDTO parseCallback(HttpServletRequest request) throws WxPayException {
        String xmlData = getRequestXml(request);
        log.debug("微信支付回调原始数据: {}", xmlData);

        WxPayOrderNotifyResult notifyResult = wxPayService.parseOrderNotifyResult(xmlData);

        WxPayCallbackDTO callback = new WxPayCallbackDTO();
        callback.setReturnCode(notifyResult.getReturnCode());
        callback.setResultCode(notifyResult.getResultCode());
        callback.setAppId(notifyResult.getAppid());
        callback.setMchId(notifyResult.getMchId());
        callback.setNonceStr(notifyResult.getNonceStr());
        callback.setSign(notifyResult.getSign());
        callback.setOutTradeNo(notifyResult.getOutTradeNo());
        callback.setTransactionId(notifyResult.getTransactionId());
        callback.setTotalFee(fenToYuan(notifyResult.getTotalFee()));
        callback.setTimeEnd(notifyResult.getTimeEnd());
        callback.setSignValid(true); // SDK 已经验证过签名

        return callback;
    }

    /**
     * 生成回调成功响应
     */
    public String successResponse() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    /**
     * 生成回调失败响应
     */
    public String failResponse(String msg) {
        return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[" + msg + "]]></return_msg></xml>";
    }

    /**
     * 从请求中获取 XML 数据
     */
    private String getRequestXml(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            log.error("读取微信回调数据失败", e);
            return null;
        }
    }
}
