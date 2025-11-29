package com.github.nonfou.mpay.controller;

import com.alipay.api.AlipayApiException;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.payment.dto.alipay.*;
import com.github.nonfou.mpay.payment.dto.wxpay.*;
import com.github.nonfou.mpay.payment.service.AlipayService;
import com.github.nonfou.mpay.payment.service.PaymentCallbackService;
import com.github.nonfou.mpay.payment.service.WxPayServiceWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器 - 整合所有支付接口
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCallbackService callbackService;

    @Autowired(required = false)
    private AlipayService alipayService;

    @Autowired(required = false)
    private WxPayServiceWrapper wxPayService;

    // ==================== 支付宝接口 ====================

    /**
     * 支付宝二维码支付
     */
    @PostMapping("/alipay/qrcode")
    public ApiResponse<AlipayQrcodeResponse> alipayQrcode(@RequestBody AlipayQrcodeRequest request) {
        if (alipayService == null) {
            return ApiResponse.error("ALIPAY_NOT_CONFIGURED", "支付宝未配置");
        }
        try {
            AlipayQrcodeResponse response = alipayService.createQrcode(request);
            if (response.isSuccess()) {
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error(response.getSubCode(), response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝二维码支付失败: {}", e.getMessage(), e);
            return ApiResponse.error("ALIPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 支付宝 H5 支付
     */
    @PostMapping("/alipay/h5")
    public ApiResponse<String> alipayH5(@RequestBody AlipayH5PayRequest request) {
        if (alipayService == null) {
            return ApiResponse.error("ALIPAY_NOT_CONFIGURED", "支付宝未配置");
        }
        try {
            String htmlForm = alipayService.createH5Pay(request);
            return ApiResponse.success(htmlForm);
        } catch (AlipayApiException e) {
            log.error("支付宝 H5 支付失败: {}", e.getMessage(), e);
            return ApiResponse.error("ALIPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 支付宝 PC 网站支付
     */
    @PostMapping("/alipay/pc")
    public ApiResponse<String> alipayPc(@RequestBody AlipayPcPayRequest request) {
        if (alipayService == null) {
            return ApiResponse.error("ALIPAY_NOT_CONFIGURED", "支付宝未配置");
        }
        try {
            String htmlForm = alipayService.createPcPay(request);
            return ApiResponse.success(htmlForm);
        } catch (AlipayApiException e) {
            log.error("支付宝 PC 支付失败: {}", e.getMessage(), e);
            return ApiResponse.error("ALIPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 支付宝退款
     */
    @PostMapping("/alipay/refund")
    public ApiResponse<AlipayRefundResponse> alipayRefund(@RequestBody AlipayRefundRequest request) {
        if (alipayService == null) {
            return ApiResponse.error("ALIPAY_NOT_CONFIGURED", "支付宝未配置");
        }
        try {
            AlipayRefundResponse response = alipayService.refund(request);
            if (response.isSuccess()) {
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error(response.getSubCode(), response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝退款失败: {}", e.getMessage(), e);
            return ApiResponse.error("ALIPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 支付宝订单查询
     */
    @PostMapping("/alipay/query")
    public ApiResponse<AlipayQueryResponse> alipayQuery(@RequestBody AlipayQueryRequest request) {
        if (alipayService == null) {
            return ApiResponse.error("ALIPAY_NOT_CONFIGURED", "支付宝未配置");
        }
        try {
            AlipayQueryResponse response = alipayService.queryOrder(request);
            if (response.isSuccess()) {
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error(response.getSubCode(), response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝订单查询失败: {}", e.getMessage(), e);
            return ApiResponse.error("ALIPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 支付宝支付回调
     */
    @PostMapping("/alipay/callback")
    public String alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝回调");
        if (alipayService == null) {
            log.error("支付宝未配置");
            return "FAIL";
        }
        try {
            AlipayCallbackDTO callback = alipayService.verifyCallback(request);
            boolean success = callbackService.handleAlipayCallback(callback);
            return success ? "SUCCESS" : "FAIL";
        } catch (AlipayApiException e) {
            log.error("支付宝回调处理失败: {}", e.getMessage(), e);
            return "FAIL";
        }
    }

    // ==================== 微信支付接口 ====================

    /**
     * 微信二维码支付
     */
    @PostMapping("/wxpay/qrcode")
    public ApiResponse<WxPayUnifiedOrderResponse> wxPayQrcode(
            @RequestBody WxPayUnifiedOrderRequest request,
            HttpServletRequest httpRequest) {
        if (wxPayService == null) {
            return ApiResponse.error("WXPAY_NOT_CONFIGURED", "微信支付未配置");
        }
        try {
            WxPayUnifiedOrderResponse response = wxPayService.createQrcode(request, httpRequest);
            if (response.isSuccess()) {
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error(response.getErrCode(), response.getErrCodeDes());
            }
        } catch (WxPayException e) {
            log.error("微信二维码��付失败: {}", e.getMessage(), e);
            return ApiResponse.error("WXPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 微信 H5 支��
     */
    @PostMapping("/wxpay/h5")
    public ApiResponse<WxPayUnifiedOrderResponse> wxPayH5(
            @RequestBody WxPayUnifiedOrderRequest request,
            HttpServletRequest httpRequest) {
        if (wxPayService == null) {
            return ApiResponse.error("WXPAY_NOT_CONFIGURED", "微信支付未配置");
        }
        try {
            WxPayUnifiedOrderResponse response = wxPayService.createH5Pay(request, httpRequest);
            if (response.isSuccess()) {
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error(response.getErrCode(), response.getErrCodeDes());
            }
        } catch (WxPayException e) {
            log.error("微信 H5 支付失败: {}", e.getMessage(), e);
            return ApiResponse.error("WXPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 微信退款
     */
    @PostMapping("/wxpay/refund")
    public ApiResponse<WxPayRefundResponse> wxPayRefund(@RequestBody WxPayRefundRequest request) {
        if (wxPayService == null) {
            return ApiResponse.error("WXPAY_NOT_CONFIGURED", "微信支付未配置");
        }
        try {
            WxPayRefundResponse response = wxPayService.refund(request);
            if (response.isSuccess()) {
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error(response.getErrCode(), response.getErrCodeDes());
            }
        } catch (WxPayException e) {
            log.error("微信退款失败: {}", e.getMessage(), e);
            return ApiResponse.error("WXPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 微信订单查询
     */
    @PostMapping("/wxpay/query")
    public ApiResponse<WxPayQueryResponse> wxPayQuery(@RequestBody WxPayQueryRequest request) {
        if (wxPayService == null) {
            return ApiResponse.error("WXPAY_NOT_CONFIGURED", "微信支付未配置");
        }
        try {
            WxPayQueryResponse response = wxPayService.queryOrder(request);
            if (response.isSuccess()) {
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error(response.getErrCode(), response.getErrCodeDes());
            }
        } catch (WxPayException e) {
            log.error("微信订单查询失败: {}", e.getMessage(), e);
            return ApiResponse.error("WXPAY_ERROR", e.getMessage());
        }
    }

    /**
     * 微信支付回调
     */
    @PostMapping(value = "/wxpay/callback", produces = MediaType.APPLICATION_XML_VALUE)
    public String wxPayCallback(HttpServletRequest request) {
        log.info("收到微信支付回调");
        if (wxPayService == null) {
            log.error("微信支付未配置");
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[微信支付未配置]]></return_msg></xml>";
        }
        try {
            WxPayCallbackDTO callback = wxPayService.parseCallback(request);
            boolean success = callbackService.handleWxPayCallback(callback);
            return success ? wxPayService.successResponse() : wxPayService.failResponse("处理失败");
        } catch (WxPayException e) {
            log.error("微信支付回调处理失败: {}", e.getMessage(), e);
            return wxPayService.failResponse(e.getMessage());
        }
    }

    /**
     * 微信退款回调
     */
    @PostMapping(value = "/wxpay/refund-callback", produces = MediaType.APPLICATION_XML_VALUE)
    public String wxRefundCallback(HttpServletRequest request) {
        log.info("收到微信退款回调");
        if (wxPayService == null) {
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[微信支付未配置]]></return_msg></xml>";
        }
        // TODO: 实现退款回调处理逻辑
        return wxPayService.successResponse();
    }

    // ==================== 状态查询接口 ====================

    /**
     * 获取支付渠道配置状态
     */
    @GetMapping("/status")
    public ApiResponse<PaymentStatus> getStatus() {
        PaymentStatus status = new PaymentStatus(
                alipayService != null,
                wxPayService != null
        );
        return ApiResponse.success(status);
    }

    public record PaymentStatus(boolean alipayConfigured, boolean wxpayConfigured) {}
}
