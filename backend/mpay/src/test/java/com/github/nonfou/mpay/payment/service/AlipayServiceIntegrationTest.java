package com.github.nonfou.mpay.payment.service;

import com.github.nonfou.mpay.payment.dto.alipay.*;
import com.github.nonfou.mpay.payment.properties.AlipayProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 支付宝服务集成测试
 *
 * 这些测试会真正调用支付宝沙箱环境 API
 * 需要配置有效的支付宝沙箱凭证才能���行
 *
 * 运行方式：
 * 1. 配置环境变量：ALIPAY_APP_ID, ALIPAY_PRIVATE_KEY, ALIPAY_PUBLIC_KEY
 * 2. 或者在 application-test.yml 中配置
 * 3. 运行: mvn test -Dtest=AlipayServiceIntegrationTest -Dspring.profiles.active=integration
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("支付宝集成测试（沙箱环境）")
@Disabled("集成测试需要配置支付宝凭证，默认跳过。移除 @Disabled 并配置凭证后运行")
class AlipayServiceIntegrationTest {

    @Autowired(required = false)
    private AlipayService alipayService;

    @Autowired(required = false)
    private AlipayProperties alipayProperties;

    // 存储测试过程中创建的订单号，用于后续退款测试
    private static String testOutTradeNo;
    private static String testTradeNo;

    @BeforeEach
    void setUp() {
        // 如果支付宝未配置，跳过测试
        assumeTrue(alipayService != null, "支付宝服务未配置，跳过集成测试");
        assumeTrue(alipayProperties != null && alipayProperties.getAppId() != null,
                "支付宝配置不完整，跳过集成测试");
    }

    @Test
    @Order(1)
    @DisplayName("1. 创建二维码支付订单")
    void testCreateQrcode() throws Exception {
        testOutTradeNo = "TEST_" + System.currentTimeMillis();

        AlipayQrcodeRequest request = new AlipayQrcodeRequest()
                .setOutTradeNo(testOutTradeNo)
                .setTotalAmount(new BigDecimal("0.01"))
                .setSubject("集成测试��品")
                .setTimeoutExpress("30m");

        AlipayQrcodeResponse response = alipayService.createQrcode(request);

        System.out.println("=== 二维码支付响应 ===");
        System.out.println("Code: " + response.getCode());
        System.out.println("Msg: " + response.getMsg());
        System.out.println("OutTradeNo: " + response.getOutTradeNo());
        System.out.println("QrCode: " + response.getQrCode());

        if (response.isSuccess()) {
            assertThat(response.getQrCode()).isNotBlank();
            System.out.println("\n请使用支付宝沙箱App扫描以下二维码完成支付:");
            System.out.println(response.getQrCode());
        } else {
            System.out.println("SubCode: " + response.getSubCode());
            System.out.println("SubMsg: " + response.getSubMsg());
        }
    }

    @Test
    @Order(2)
    @DisplayName("2. 创建PC网站支付")
    void testCreatePcPay() throws Exception {
        String orderNo = "PC_TEST_" + System.currentTimeMillis();

        AlipayPcPayRequest request = new AlipayPcPayRequest()
                .setOutTradeNo(orderNo)
                .setTotalAmount(new BigDecimal("0.01"))
                .setSubject("PC测试商品")
                .setBody("这是一个测试商品描述")
                .setTimeoutExpress("30m");

        String htmlForm = alipayService.createPcPay(request);

        System.out.println("=== PC支付响应（HTML表单）===");
        System.out.println(htmlForm.substring(0, Math.min(500, htmlForm.length())) + "...");

        assertThat(htmlForm).isNotBlank();
        assertThat(htmlForm).contains("form");
    }

    @Test
    @Order(3)
    @DisplayName("3. 创建H5手机支付")
    void testCreateH5Pay() throws Exception {
        String orderNo = "H5_TEST_" + System.currentTimeMillis();

        AlipayH5PayRequest request = new AlipayH5PayRequest()
                .setOutTradeNo(orderNo)
                .setTotalAmount(new BigDecimal("0.01"))
                .setSubject("H5测试商品")
                .setQuitUrl("http://localhost:5173/quit")
                .setTimeoutExpress("30m");

        String htmlForm = alipayService.createH5Pay(request);

        System.out.println("=== H5支付响应（HTML表单）===");
        System.out.println(htmlForm.substring(0, Math.min(500, htmlForm.length())) + "...");

        assertThat(htmlForm).isNotBlank();
        assertThat(htmlForm).contains("form");
    }

    @Test
    @Order(4)
    @DisplayName("4. 退款测试 - 交易不存在")
    void testRefund_TradeNotExist() throws Exception {
        AlipayRefundRequest request = new AlipayRefundRequest()
                .setOutTradeNo("NOT_EXIST_ORDER_" + System.currentTimeMillis())
                .setRefundAmount(new BigDecimal("0.01"))
                .setRefundReason("测试退款");

        AlipayRefundResponse response = alipayService.refund(request);

        System.out.println("=== 退款响应（交易不存在）===");
        System.out.println("Code: " + response.getCode());
        System.out.println("Msg: " + response.getMsg());
        System.out.println("SubCode: " + response.getSubCode());
        System.out.println("SubMsg: " + response.getSubMsg());

        // 预期：交易不存在
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getSubCode()).isEqualTo("ACQ.TRADE_NOT_EXIST");
    }

    @Test
    @Order(5)
    @DisplayName("5. 关闭订单测试")
    void testCloseOrder() throws Exception {
        // 先创建一个订单
        String orderNo = "CLOSE_TEST_" + System.currentTimeMillis();

        AlipayQrcodeRequest createRequest = new AlipayQrcodeRequest()
                .setOutTradeNo(orderNo)
                .setTotalAmount(new BigDecimal("0.01"))
                .setSubject("待关闭订单");

        AlipayQrcodeResponse createResponse = alipayService.createQrcode(createRequest);

        if (createResponse.isSuccess()) {
            // 关闭订单
            boolean closed = alipayService.closeOrder(orderNo);
            System.out.println("=== 关闭订单结果 ===");
            System.out.println("订单号: " + orderNo);
            System.out.println("关闭结果: " + (closed ? "成功" : "失败"));
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. 退款测试（需要已支付订单）- 手动执行")
    @Disabled("需要先完成支付，请手动启用此测试")
    void testRefund_Success() throws Exception {
        // 这个测试需要有一个已支付的订单
        // 请先运行 testCreateQrcode，使用支付宝沙箱App完成支付
        // 然后手动设置 testOutTradeNo 和 testTradeNo

        assumeTrue(testOutTradeNo != null, "需要已支付订单号");

        AlipayRefundRequest request = new AlipayRefundRequest()
                .setOutTradeNo(testOutTradeNo)
                .setRefundAmount(new BigDecimal("0.01"))
                .setRefundReason("集成测试退款");

        AlipayRefundResponse response = alipayService.refund(request);

        System.out.println("=== 退款响应 ===");
        System.out.println("Code: " + response.getCode());
        System.out.println("Msg: " + response.getMsg());
        System.out.println("TradeNo: " + response.getTradeNo());
        System.out.println("OutTradeNo: " + response.getOutTradeNo());
        System.out.println("RefundFee: " + response.getRefundFee());

        if (!response.isSuccess()) {
            System.out.println("SubCode: " + response.getSubCode());
            System.out.println("SubMsg: " + response.getSubMsg());
        }
    }
}
