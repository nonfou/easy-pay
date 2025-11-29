package com.github.nonfou.mpay.controller;

import com.alipay.api.AlipayApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nonfou.mpay.payment.dto.alipay.*;
import com.github.nonfou.mpay.payment.service.AlipayService;
import com.github.nonfou.mpay.payment.service.PaymentCallbackService;
import com.github.nonfou.mpay.payment.service.WxPayServiceWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PaymentController 单元测试
 * 测试支付宝相关的 REST 接口
 *
 * 注意：ApiResponse 使用 code=0 表示成功，code=-1 表示错误
 */
@WebMvcTest(PaymentController.class)
@DisplayName("支付控制器测试")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlipayService alipayService;

    @MockBean
    private WxPayServiceWrapper wxPayService;

    @MockBean
    private PaymentCallbackService callbackService;

    // ==================== 支付宝二维码支付接口测试 ====================
    @Nested
    @DisplayName("支付宝二维码支付接口")
    class AlipayQrcodeTests {

        @Test
        @DisplayName("二维码支付成功")
        void alipayQrcode_Success() throws Exception {
            AlipayQrcodeResponse response = new AlipayQrcodeResponse();
            response.setCode("10000");
            response.setMsg("Success");
            response.setOutTradeNo("ORDER_001");
            response.setQrCode("https://qr.alipay.com/test123");

            when(alipayService.createQrcode(any(AlipayQrcodeRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/qrcode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "totalAmount": 10.00,
                                    "subject": "测试商品",
                                    "timeoutExpress": "30m"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))  // 0 表示成功
                    .andExpect(jsonPath("$.data.code").value("10000"))
                    .andExpect(jsonPath("$.data.out_trade_no").value("ORDER_001"))
                    .andExpect(jsonPath("$.data.qr_code").value("https://qr.alipay.com/test123"));
        }

        @Test
        @DisplayName("二维码支付失败 - 业务错误")
        void alipayQrcode_BusinessError() throws Exception {
            AlipayQrcodeResponse response = new AlipayQrcodeResponse();
            response.setCode("40004");
            response.setMsg("Business Failed");
            response.setSubCode("ACQ.TRADE_HAS_SUCCESS");
            response.setSubMsg("交易已被支付");

            when(alipayService.createQrcode(any(AlipayQrcodeRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/qrcode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "totalAmount": 10.00,
                                    "subject": "测试商品"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1))  // -1 表示错误
                    .andExpect(jsonPath("$.msg").value(containsString("交易已被支付")));
        }

        @Test
        @DisplayName("二维码支付异常 - AlipayApiException")
        void alipayQrcode_Exception() throws Exception {
            when(alipayService.createQrcode(any(AlipayQrcodeRequest.class)))
                    .thenThrow(new AlipayApiException("网络连接超时"));

            mockMvc.perform(post("/api/payment/alipay/qrcode")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "totalAmount": 10.00,
                                    "subject": "测试商品"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1))
                    .andExpect(jsonPath("$.msg").value(containsString("网络连接超时")));
        }
    }

    // ==================== 支付宝 PC 支付接口测试 ====================
    @Nested
    @DisplayName("支付宝PC支付接口")
    class AlipayPcTests {

        @Test
        @DisplayName("PC支付成功 - 返回HTML表单")
        void alipayPc_Success() throws Exception {
            String htmlForm = "<form id='alipaysubmit'><input type='submit'></form>";
            when(alipayService.createPcPay(any(AlipayPcPayRequest.class))).thenReturn(htmlForm);

            mockMvc.perform(post("/api/payment/alipay/pc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "PC_ORDER_001",
                                    "totalAmount": 100.00,
                                    "subject": "PC测试商品",
                                    "body": "商品描述"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value(containsString("form")));
        }

        @Test
        @DisplayName("PC支付异常")
        void alipayPc_Exception() throws Exception {
            when(alipayService.createPcPay(any(AlipayPcPayRequest.class)))
                    .thenThrow(new AlipayApiException("系统繁忙"));

            mockMvc.perform(post("/api/payment/alipay/pc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "PC_ORDER_001",
                                    "totalAmount": 100.00,
                                    "subject": "测试"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1));
        }
    }

    // ==================== 支付宝 H5 支付接口测试 ====================
    @Nested
    @DisplayName("支付宝H5支付接口")
    class AlipayH5Tests {

        @Test
        @DisplayName("H5支付成功")
        void alipayH5_Success() throws Exception {
            String htmlForm = "<form id='alipaysubmit'></form>";
            when(alipayService.createH5Pay(any(AlipayH5PayRequest.class))).thenReturn(htmlForm);

            mockMvc.perform(post("/api/payment/alipay/h5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "H5_ORDER_001",
                                    "totalAmount": 50.00,
                                    "subject": "H5测试商品",
                                    "quitUrl": "http://localhost:5173/quit"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isNotEmpty());
        }
    }

    // ==================== 支付宝退款接口测试 ====================
    @Nested
    @DisplayName("支付宝退款接口")
    class AlipayRefundTests {

        @Test
        @DisplayName("全额退款成功")
        void alipayRefund_FullRefund_Success() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("10000");
            response.setMsg("Success");
            response.setTradeNo("2024112900001");
            response.setOutTradeNo("ORDER_001");
            response.setRefundFee(new BigDecimal("100.00"));

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "refundAmount": 100.00,
                                    "refundReason": "用户申请退款"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.code").value("10000"))
                    .andExpect(jsonPath("$.data.trade_no").value("2024112900001"))
                    .andExpect(jsonPath("$.data.refund_fee").value(100.00));
        }

        @Test
        @DisplayName("部分退款成功")
        void alipayRefund_PartialRefund_Success() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("10000");
            response.setMsg("Success");
            response.setTradeNo("2024112900002");
            response.setOutTradeNo("ORDER_002");
            response.setRefundFee(new BigDecimal("30.00"));

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_002",
                                    "refundAmount": 30.00,
                                    "refundReason": "部分退款",
                                    "outRequestNo": "REFUND_001"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.refund_fee").value(30.00));
        }

        @Test
        @DisplayName("使用支付宝交易号退款")
        void alipayRefund_ByTradeNo_Success() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("10000");
            response.setMsg("Success");
            response.setTradeNo("2024112900003");
            response.setRefundFee(new BigDecimal("50.00"));

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeNo": "2024112900003",
                                    "refundAmount": 50.00
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.code").value("10000"));
        }

        @Test
        @DisplayName("退款失败 - 交易不存在")
        void alipayRefund_TradeNotExist() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("40004");
            response.setMsg("Business Failed");
            response.setSubCode("ACQ.TRADE_NOT_EXIST");
            response.setSubMsg("交易不存在");

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "NOT_EXIST_ORDER",
                                    "refundAmount": 10.00
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1))
                    .andExpect(jsonPath("$.msg").value(containsString("交易不存在")));
        }

        @Test
        @DisplayName("退款失败 - 交易状态不允许")
        void alipayRefund_TradeStatusError() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("40004");
            response.setMsg("Business Failed");
            response.setSubCode("ACQ.TRADE_STATUS_ERROR");
            response.setSubMsg("交易状态不合法");

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "UNPAID_ORDER",
                                    "refundAmount": 10.00
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1))
                    .andExpect(jsonPath("$.msg").value(containsString("交易状态不合法")));
        }

        @Test
        @DisplayName("退款失败 - 金额超限")
        void alipayRefund_AmountExceed() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("40004");
            response.setMsg("Business Failed");
            response.setSubCode("ACQ.REFUND_AMT_NOT_EQUAL_TOTAL");
            response.setSubMsg("退款金额超过可退金额");

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "refundAmount": 9999.00
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1))
                    .andExpect(jsonPath("$.msg").value(containsString("退款金额超过可退金额")));
        }

        @Test
        @DisplayName("退款异常 - 网络超时")
        void alipayRefund_NetworkTimeout() throws Exception {
            when(alipayService.refund(any(AlipayRefundRequest.class)))
                    .thenThrow(new AlipayApiException("Read timed out"));

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "refundAmount": 10.00
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1))
                    .andExpect(jsonPath("$.msg").value(containsString("Read timed out")));
        }

        @Test
        @DisplayName("退款 - 最小金额0.01元")
        void alipayRefund_MinimumAmount() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("10000");
            response.setMsg("Success");
            response.setRefundFee(new BigDecimal("0.01"));

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "refundAmount": 0.01
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.refund_fee").value(0.01));
        }
    }

    // ==================== 支付宝回调接口测试 ====================
    @Nested
    @DisplayName("支付宝回调接口")
    class AlipayCallbackTests {

        @Test
        @DisplayName("回调处理成功")
        void alipayCallback_Success() throws Exception {
            AlipayCallbackDTO callback = new AlipayCallbackDTO();
            callback.setOutTradeNo("ORDER_001");
            callback.setTradeNo("2024112900001");
            callback.setTradeStatus("TRADE_SUCCESS");
            callback.setTotalAmount(new BigDecimal("100.00"));

            when(alipayService.verifyCallback(any())).thenReturn(callback);
            when(callbackService.handleAlipayCallback(callback)).thenReturn(true);

            mockMvc.perform(post("/api/payment/alipay/callback")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("out_trade_no", "ORDER_001")
                            .param("trade_no", "2024112900001")
                            .param("trade_status", "TRADE_SUCCESS")
                            .param("total_amount", "100.00"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("SUCCESS"));
        }

        @Test
        @DisplayName("回调处理失败")
        void alipayCallback_ProcessFailed() throws Exception {
            AlipayCallbackDTO callback = new AlipayCallbackDTO();
            callback.setOutTradeNo("ORDER_001");
            callback.setTradeStatus("TRADE_SUCCESS");

            when(alipayService.verifyCallback(any())).thenReturn(callback);
            when(callbackService.handleAlipayCallback(callback)).thenReturn(false);

            mockMvc.perform(post("/api/payment/alipay/callback")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("out_trade_no", "ORDER_001"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("FAIL"));
        }

        @Test
        @DisplayName("回调签名验证失败")
        void alipayCallback_SignVerifyFailed() throws Exception {
            when(alipayService.verifyCallback(any()))
                    .thenThrow(new AlipayApiException("签名验证失败"));

            mockMvc.perform(post("/api/payment/alipay/callback")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("out_trade_no", "ORDER_001"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("FAIL"));
        }
    }

    // ==================== 支付状态接口测试 ====================
    @Nested
    @DisplayName("支付状态接口")
    class PaymentStatusTests {

        @Test
        @DisplayName("获取支付配置状态 - 支付宝和微信都已配置")
        void getStatus_BothConfigured() throws Exception {
            // alipayService 和 wxPayService 都不为 null（MockBean 默认注入）

            mockMvc.perform(get("/api/payment/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.alipayConfigured").value(true))
                    .andExpect(jsonPath("$.data.wxpayConfigured").value(true));
        }
    }

    // ==================== 边界情况测试 ====================
    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("请求参数验证 - 无效的JSON")
        void alipayRefund_InvalidJson() throws Exception {
            // 无效 JSON 导致解析异常，根据全局异常处理可能返回 400 或 500
            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("invalid json"))
                    .andExpect(status().isInternalServerError()); // 当前实现返回 500
        }

        @Test
        @DisplayName("请求参数 - 空JSON对象")
        void alipayRefund_EmptyJson() throws Exception {
            // 空 JSON 对象会导致 refundAmount 为 null，这取决于业务验证
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("10000");

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("大金额退款")
        void alipayRefund_LargeAmount() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("10000");
            response.setMsg("Success");
            response.setRefundFee(new BigDecimal("999999.99"));

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "LARGE_ORDER",
                                    "refundAmount": 999999.99
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.refund_fee").value(999999.99));
        }

        @Test
        @DisplayName("特殊字符退款原因")
        void alipayRefund_SpecialCharacters() throws Exception {
            AlipayRefundResponse response = new AlipayRefundResponse();
            response.setCode("10000");
            response.setMsg("Success");

            when(alipayService.refund(any(AlipayRefundRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/payment/alipay/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "outTradeNo": "ORDER_001",
                                    "refundAmount": 10.00,
                                    "refundReason": "用户退款【测试】- 特殊字符!@#$%"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
