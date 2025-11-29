package com.github.nonfou.mpay.payment.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.github.nonfou.mpay.payment.dto.alipay.*;
import com.github.nonfou.mpay.payment.properties.AlipayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlipayService 单元测试
 * 测试支付宝各种支付场景：二维码支付、PC支付、H5支付、退款、关闭订单
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("支付宝服务测试")
class AlipayServiceTest {

    @Mock
    private AlipayClient alipayClient;

    @Mock
    private AlipayProperties alipayProperties;

    @InjectMocks
    private AlipayService alipayService;

    @BeforeEach
    void setUp() {
        // 设置默认的配置属性
        lenient().when(alipayProperties.getNotifyUrl()).thenReturn("http://localhost:8080/api/payment/alipay/callback");
        lenient().when(alipayProperties.getReturnUrl()).thenReturn("http://localhost:5173/result");
        lenient().when(alipayProperties.getPublicKey()).thenReturn("test-public-key");
        lenient().when(alipayProperties.getCharset()).thenReturn("utf-8");
        lenient().when(alipayProperties.getSignType()).thenReturn("RSA2");
    }

    // ==================== 二维码支付测试 ====================
    @Nested
    @DisplayName("二维码支付测试")
    class QrcodePaymentTests {

        @Test
        @DisplayName("二维码支付成功")
        void createQrcode_Success() throws AlipayApiException {
            // 准备请求
            AlipayQrcodeRequest request = new AlipayQrcodeRequest()
                    .setOutTradeNo("TEST_ORDER_001")
                    .setTotalAmount(new BigDecimal("10.00"))
                    .setSubject("测试商品")
                    .setTimeoutExpress("30m");

            // 模拟支付宝响应
            AlipayTradePrecreateResponse mockResponse = mock(AlipayTradePrecreateResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getMsg()).thenReturn("Success");
            when(mockResponse.getOutTradeNo()).thenReturn("TEST_ORDER_001");
            when(mockResponse.getQrCode()).thenReturn("https://qr.alipay.com/test123");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"10000\"}");

            when(alipayClient.execute(any(AlipayTradePrecreateRequest.class))).thenReturn(mockResponse);

            // 执行
            AlipayQrcodeResponse response = alipayService.createQrcode(request);

            // 验证
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("10000");
            assertThat(response.getOutTradeNo()).isEqualTo("TEST_ORDER_001");
            assertThat(response.getQrCode()).isEqualTo("https://qr.alipay.com/test123");

            // 验证调用参数
            ArgumentCaptor<AlipayTradePrecreateRequest> captor = ArgumentCaptor.forClass(AlipayTradePrecreateRequest.class);
            verify(alipayClient).execute(captor.capture());
            assertThat(captor.getValue().getNotifyUrl()).isEqualTo("http://localhost:8080/api/payment/alipay/callback");
        }

        @Test
        @DisplayName("二维码支付失败 - 商户订单号重复")
        void createQrcode_DuplicateOrderNo() throws AlipayApiException {
            AlipayQrcodeRequest request = new AlipayQrcodeRequest()
                    .setOutTradeNo("DUPLICATE_ORDER")
                    .setTotalAmount(new BigDecimal("10.00"))
                    .setSubject("测试商品");

            AlipayTradePrecreateResponse mockResponse = mock(AlipayTradePrecreateResponse.class);
            when(mockResponse.getCode()).thenReturn("40004");
            when(mockResponse.getMsg()).thenReturn("Business Failed");
            when(mockResponse.getSubCode()).thenReturn("ACQ.TRADE_HAS_SUCCESS");
            when(mockResponse.getSubMsg()).thenReturn("交易已被支付");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40004\"}");

            when(alipayClient.execute(any(AlipayTradePrecreateRequest.class))).thenReturn(mockResponse);

            AlipayQrcodeResponse response = alipayService.createQrcode(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getSubCode()).isEqualTo("ACQ.TRADE_HAS_SUCCESS");
        }

        @Test
        @DisplayName("二维码支付异常 - 网络错误")
        void createQrcode_NetworkError() throws AlipayApiException {
            AlipayQrcodeRequest request = new AlipayQrcodeRequest()
                    .setOutTradeNo("TEST_ORDER")
                    .setTotalAmount(new BigDecimal("10.00"))
                    .setSubject("测试商品");

            when(alipayClient.execute(any(AlipayTradePrecreateRequest.class)))
                    .thenThrow(new AlipayApiException("网络连接超时"));

            assertThatThrownBy(() -> alipayService.createQrcode(request))
                    .isInstanceOf(AlipayApiException.class)
                    .hasMessageContaining("网络连接超时");
        }
    }

    // ==================== PC 支付测试 ====================
    @Nested
    @DisplayName("PC网站支付测试")
    class PcPaymentTests {

        @Test
        @DisplayName("PC支付成功 - 返回HTML表单")
        void createPcPay_Success() throws AlipayApiException {
            AlipayPcPayRequest request = new AlipayPcPayRequest()
                    .setOutTradeNo("PC_ORDER_001")
                    .setTotalAmount(new BigDecimal("100.00"))
                    .setSubject("PC端测试商品")
                    .setBody("商品描述")
                    .setTimeoutExpress("30m");

            String expectedHtml = "<form id='alipaysubmit' action='https://openapi.alipay.com/gateway.do'>" +
                    "<input type='hidden' name='biz_content' value='{}'></form>";

            AlipayTradePagePayResponse mockResponse = mock(AlipayTradePagePayResponse.class);
            when(mockResponse.getBody()).thenReturn(expectedHtml);

            when(alipayClient.pageExecute(any(AlipayTradePagePayRequest.class))).thenReturn(mockResponse);

            String htmlForm = alipayService.createPcPay(request);

            assertThat(htmlForm).isNotNull();
            assertThat(htmlForm).contains("form");
            assertThat(htmlForm).contains("alipaysubmit");

            // 验证配置被正确使用
            ArgumentCaptor<AlipayTradePagePayRequest> captor = ArgumentCaptor.forClass(AlipayTradePagePayRequest.class);
            verify(alipayClient).pageExecute(captor.capture());
            assertThat(captor.getValue().getReturnUrl()).isEqualTo("http://localhost:5173/result");
            assertThat(captor.getValue().getNotifyUrl()).isEqualTo("http://localhost:8080/api/payment/alipay/callback");
        }

        @Test
        @DisplayName("PC支付 - 无商品描述")
        void createPcPay_WithoutBody() throws AlipayApiException {
            AlipayPcPayRequest request = new AlipayPcPayRequest()
                    .setOutTradeNo("PC_ORDER_002")
                    .setTotalAmount(new BigDecimal("50.00"))
                    .setSubject("测试商品");

            AlipayTradePagePayResponse mockResponse = mock(AlipayTradePagePayResponse.class);
            when(mockResponse.getBody()).thenReturn("<form></form>");

            when(alipayClient.pageExecute(any(AlipayTradePagePayRequest.class))).thenReturn(mockResponse);

            String htmlForm = alipayService.createPcPay(request);

            assertThat(htmlForm).isNotNull();
        }
    }

    // ==================== H5 支付测试 ====================
    @Nested
    @DisplayName("H5手机支付测试")
    class H5PaymentTests {

        @Test
        @DisplayName("H5支付成功")
        void createH5Pay_Success() throws AlipayApiException {
            AlipayH5PayRequest request = new AlipayH5PayRequest()
                    .setOutTradeNo("H5_ORDER_001")
                    .setTotalAmount(new BigDecimal("25.50"))
                    .setSubject("H5测试商品")
                    .setQuitUrl("http://localhost:5173/cancel");

            String expectedHtml = "<form id='alipaysubmit' action='https://openapi.alipay.com/gateway.do'></form>";

            AlipayTradeWapPayResponse mockResponse = mock(AlipayTradeWapPayResponse.class);
            when(mockResponse.getBody()).thenReturn(expectedHtml);

            when(alipayClient.pageExecute(any(AlipayTradeWapPayRequest.class))).thenReturn(mockResponse);

            String htmlForm = alipayService.createH5Pay(request);

            assertThat(htmlForm).isNotNull();
            assertThat(htmlForm).contains("form");
        }

        @Test
        @DisplayName("H5支付 - 带退出URL")
        void createH5Pay_WithQuitUrl() throws AlipayApiException {
            AlipayH5PayRequest request = new AlipayH5PayRequest()
                    .setOutTradeNo("H5_ORDER_002")
                    .setTotalAmount(new BigDecimal("30.00"))
                    .setSubject("测试")
                    .setQuitUrl("http://localhost:5173/quit");

            AlipayTradeWapPayResponse mockResponse = mock(AlipayTradeWapPayResponse.class);
            when(mockResponse.getBody()).thenReturn("<form></form>");

            when(alipayClient.pageExecute(any(AlipayTradeWapPayRequest.class))).thenReturn(mockResponse);

            String htmlForm = alipayService.createH5Pay(request);

            assertThat(htmlForm).isNotNull();
        }
    }

    // ==================== 退款测试 ====================
    @Nested
    @DisplayName("退款测试")
    class RefundTests {

        @Test
        @DisplayName("全额退款成功 - 使用商户订单号")
        void refund_FullRefund_ByOutTradeNo_Success() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_001")
                    .setRefundAmount(new BigDecimal("100.00"))
                    .setRefundReason("用户申请退款");

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getMsg()).thenReturn("Success");
            when(mockResponse.getTradeNo()).thenReturn("2024112900001");
            when(mockResponse.getOutTradeNo()).thenReturn("ORDER_001");
            when(mockResponse.getBuyerUserId()).thenReturn("2088102180000000");
            when(mockResponse.getRefundFee()).thenReturn("100.00");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"10000\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("10000");
            assertThat(response.getTradeNo()).isEqualTo("2024112900001");
            assertThat(response.getOutTradeNo()).isEqualTo("ORDER_001");
            assertThat(response.getRefundFee()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("全额退款成功 - 使用支付宝交易号")
        void refund_FullRefund_ByTradeNo_Success() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setTradeNo("2024112900001")
                    .setRefundAmount(new BigDecimal("50.00"))
                    .setRefundReason("商品质量问题");

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getMsg()).thenReturn("Success");
            when(mockResponse.getTradeNo()).thenReturn("2024112900001");
            when(mockResponse.getRefundFee()).thenReturn("50.00");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"10000\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getRefundFee()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("部分退款成功 - 需要退款请求号")
        void refund_PartialRefund_Success() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_002")
                    .setRefundAmount(new BigDecimal("30.00"))
                    .setRefundReason("部分退款")
                    .setOutRequestNo("REFUND_001");

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getMsg()).thenReturn("Success");
            when(mockResponse.getTradeNo()).thenReturn("2024112900002");
            when(mockResponse.getOutTradeNo()).thenReturn("ORDER_002");
            when(mockResponse.getRefundFee()).thenReturn("30.00");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"10000\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getRefundFee()).isEqualByComparingTo(new BigDecimal("30.00"));

            // 验证请求参数包含 outRequestNo
            ArgumentCaptor<AlipayTradeRefundRequest> captor = ArgumentCaptor.forClass(AlipayTradeRefundRequest.class);
            verify(alipayClient).execute(captor.capture());
            assertThat(captor.getValue().getBizContent()).contains("out_request_no");
        }

        @Test
        @DisplayName("多次部分退款")
        void refund_MultiplePartialRefunds() throws AlipayApiException {
            // 第一次部分退款
            AlipayRefundRequest request1 = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_003")
                    .setRefundAmount(new BigDecimal("20.00"))
                    .setOutRequestNo("REFUND_001");

            AlipayTradeRefundResponse mockResponse1 = mock(AlipayTradeRefundResponse.class);
            when(mockResponse1.getCode()).thenReturn("10000");
            when(mockResponse1.getRefundFee()).thenReturn("20.00");
            when(mockResponse1.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse1);

            AlipayRefundResponse response1 = alipayService.refund(request1);
            assertThat(response1.isSuccess()).isTrue();

            // 第二次部分退款
            AlipayRefundRequest request2 = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_003")
                    .setRefundAmount(new BigDecimal("30.00"))
                    .setOutRequestNo("REFUND_002");

            AlipayTradeRefundResponse mockResponse2 = mock(AlipayTradeRefundResponse.class);
            when(mockResponse2.getCode()).thenReturn("10000");
            when(mockResponse2.getRefundFee()).thenReturn("30.00");
            when(mockResponse2.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse2);

            AlipayRefundResponse response2 = alipayService.refund(request2);
            assertThat(response2.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("退款失败 - 交易不存在")
        void refund_TradeNotExist() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("NOT_EXIST_ORDER")
                    .setRefundAmount(new BigDecimal("10.00"));

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("40004");
            when(mockResponse.getMsg()).thenReturn("Business Failed");
            when(mockResponse.getSubCode()).thenReturn("ACQ.TRADE_NOT_EXIST");
            when(mockResponse.getSubMsg()).thenReturn("交易不存在");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40004\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo("40004");
            assertThat(response.getSubCode()).isEqualTo("ACQ.TRADE_NOT_EXIST");
            assertThat(response.getSubMsg()).isEqualTo("交易不存在");
        }

        @Test
        @DisplayName("退款失败 - 交易状态不允许退款")
        void refund_TradeStatusError() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("UNPAID_ORDER")
                    .setRefundAmount(new BigDecimal("10.00"));

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("40004");
            when(mockResponse.getMsg()).thenReturn("Business Failed");
            when(mockResponse.getSubCode()).thenReturn("ACQ.TRADE_STATUS_ERROR");
            when(mockResponse.getSubMsg()).thenReturn("交易状态不合法");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40004\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getSubCode()).isEqualTo("ACQ.TRADE_STATUS_ERROR");
        }

        @Test
        @DisplayName("退款失败 - 退款金额超过可退金额")
        void refund_AmountExceed() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_004")
                    .setRefundAmount(new BigDecimal("1000.00")); // 超过订单金额

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("40004");
            when(mockResponse.getMsg()).thenReturn("Business Failed");
            when(mockResponse.getSubCode()).thenReturn("ACQ.REFUND_AMT_NOT_EQUAL_TOTAL");
            when(mockResponse.getSubMsg()).thenReturn("退款金额超过可退金额");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40004\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getSubCode()).isEqualTo("ACQ.REFUND_AMT_NOT_EQUAL_TOTAL");
        }

        @Test
        @DisplayName("退款失败 - 重复退款请求")
        void refund_DuplicateRequest() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_005")
                    .setRefundAmount(new BigDecimal("50.00"))
                    .setOutRequestNo("REFUND_DUPLICATE");

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("40004");
            when(mockResponse.getMsg()).thenReturn("Business Failed");
            when(mockResponse.getSubCode()).thenReturn("ACQ.REFUND_REASON_SAME");
            when(mockResponse.getSubMsg()).thenReturn("退款请求号重复");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40004\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getSubCode()).isEqualTo("ACQ.REFUND_REASON_SAME");
        }

        @Test
        @DisplayName("退款异常 - 网络超时")
        void refund_NetworkTimeout() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_006")
                    .setRefundAmount(new BigDecimal("10.00"));

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class)))
                    .thenThrow(new AlipayApiException("Read timed out"));

            assertThatThrownBy(() -> alipayService.refund(request))
                    .isInstanceOf(AlipayApiException.class)
                    .hasMessageContaining("timed out");
        }

        @Test
        @DisplayName("退款异常 - 签名错误")
        void refund_SignError() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_007")
                    .setRefundAmount(new BigDecimal("10.00"));

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("40002");
            when(mockResponse.getMsg()).thenReturn("Invalid Arguments");
            when(mockResponse.getSubCode()).thenReturn("isv.invalid-signature");
            when(mockResponse.getSubMsg()).thenReturn("无效签名");
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40002\"}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo("40002");
        }

        @Test
        @DisplayName("退款 - 无退款原因")
        void refund_WithoutReason() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_008")
                    .setRefundAmount(new BigDecimal("10.00"));
            // 不设置 refundReason

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getMsg()).thenReturn("Success");
            when(mockResponse.getRefundFee()).thenReturn("10.00");
            when(mockResponse.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("退款金额精度测试 - 两位小数")
        void refund_AmountPrecision() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_009")
                    .setRefundAmount(new BigDecimal("99.99"));

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getRefundFee()).thenReturn("99.99");
            when(mockResponse.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getRefundFee()).isEqualByComparingTo(new BigDecimal("99.99"));
        }

        @Test
        @DisplayName("退款 - 小额退款（0.01元）")
        void refund_MinimumAmount() throws AlipayApiException {
            AlipayRefundRequest request = new AlipayRefundRequest()
                    .setOutTradeNo("ORDER_010")
                    .setRefundAmount(new BigDecimal("0.01"));

            AlipayTradeRefundResponse mockResponse = mock(AlipayTradeRefundResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getRefundFee()).thenReturn("0.01");
            when(mockResponse.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(mockResponse);

            AlipayRefundResponse response = alipayService.refund(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getRefundFee()).isEqualByComparingTo(new BigDecimal("0.01"));
        }
    }

    // ==================== 关闭订单测试 ====================
    @Nested
    @DisplayName("关闭订单测试")
    class CloseOrderTests {

        @Test
        @DisplayName("关闭订单成功")
        void closeOrder_Success() throws AlipayApiException {
            AlipayTradeCloseResponse mockResponse = mock(AlipayTradeCloseResponse.class);
            when(mockResponse.isSuccess()).thenReturn(true);
            when(mockResponse.getBody()).thenReturn("{\"code\":\"10000\"}");

            when(alipayClient.execute(any(AlipayTradeCloseRequest.class))).thenReturn(mockResponse);

            boolean result = alipayService.closeOrder("ORDER_TO_CLOSE");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("关闭订单失败 - 订单已支付")
        void closeOrder_AlreadyPaid() throws AlipayApiException {
            AlipayTradeCloseResponse mockResponse = mock(AlipayTradeCloseResponse.class);
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40004\"}");

            when(alipayClient.execute(any(AlipayTradeCloseRequest.class))).thenReturn(mockResponse);

            boolean result = alipayService.closeOrder("PAID_ORDER");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("关闭订单失败 - 订单不存在")
        void closeOrder_NotExist() throws AlipayApiException {
            AlipayTradeCloseResponse mockResponse = mock(AlipayTradeCloseResponse.class);
            when(mockResponse.isSuccess()).thenReturn(false);
            when(mockResponse.getBody()).thenReturn("{\"code\":\"40004\"}");

            when(alipayClient.execute(any(AlipayTradeCloseRequest.class))).thenReturn(mockResponse);

            boolean result = alipayService.closeOrder("NOT_EXIST_ORDER");

            assertThat(result).isFalse();
        }
    }

    // ==================== 边界条件测试 ====================
    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("大金额支付测试")
        void largeAmount() throws AlipayApiException {
            AlipayQrcodeRequest request = new AlipayQrcodeRequest()
                    .setOutTradeNo("LARGE_ORDER")
                    .setTotalAmount(new BigDecimal("999999.99"))
                    .setSubject("大额商品");

            AlipayTradePrecreateResponse mockResponse = mock(AlipayTradePrecreateResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getQrCode()).thenReturn("https://qr.alipay.com/large");
            when(mockResponse.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradePrecreateRequest.class))).thenReturn(mockResponse);

            AlipayQrcodeResponse response = alipayService.createQrcode(request);

            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("特殊字符商品名称")
        void specialCharactersInSubject() throws AlipayApiException {
            AlipayQrcodeRequest request = new AlipayQrcodeRequest()
                    .setOutTradeNo("SPECIAL_ORDER")
                    .setTotalAmount(new BigDecimal("10.00"))
                    .setSubject("测试商品【特价】- 50%优惠！@#$");

            AlipayTradePrecreateResponse mockResponse = mock(AlipayTradePrecreateResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradePrecreateRequest.class))).thenReturn(mockResponse);

            AlipayQrcodeResponse response = alipayService.createQrcode(request);

            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("超长商户订单号")
        void longOutTradeNo() throws AlipayApiException {
            String longOrderNo = "ORDER_" + "1234567890".repeat(6); // 64字符以内

            AlipayQrcodeRequest request = new AlipayQrcodeRequest()
                    .setOutTradeNo(longOrderNo)
                    .setTotalAmount(new BigDecimal("10.00"))
                    .setSubject("测试");

            AlipayTradePrecreateResponse mockResponse = mock(AlipayTradePrecreateResponse.class);
            when(mockResponse.getCode()).thenReturn("10000");
            when(mockResponse.getOutTradeNo()).thenReturn(longOrderNo);
            when(mockResponse.getBody()).thenReturn("{}");

            when(alipayClient.execute(any(AlipayTradePrecreateRequest.class))).thenReturn(mockResponse);

            AlipayQrcodeResponse response = alipayService.createQrcode(request);

            assertThat(response.isSuccess()).isTrue();
        }
    }
}
