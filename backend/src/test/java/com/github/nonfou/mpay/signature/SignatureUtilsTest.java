package com.github.nonfou.mpay.signature;

import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SignatureUtils 单元测试
 * 测试签名字符串构建和 MD5 哈希
 */
@DisplayName("SignatureUtils 签名工具测试")
class SignatureUtilsTest {

    @Nested
    @DisplayName("buildSignString(PublicCreateOrderDTO) 测试")
    class BuildSignStringFromDTOTests {

        @Test
        @DisplayName("构建签名字符串 - 基本字段按字典序排列")
        void buildSignString_shouldSortFieldsAlphabetically() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            // 字典序: clientip, device, money, name, notify_url, out_trade_no, pid, type
            assertTrue(signString.contains("clientip=127.0.0.1"));
            assertTrue(signString.contains("device=pc"));
            assertTrue(signString.contains("money=100.00"));
            assertTrue(signString.contains("name=测试商品"));
            assertTrue(signString.contains("notify_url=http://example.com/notify"));
            assertTrue(signString.contains("out_trade_no=TEST123456"));
            assertTrue(signString.contains("pid=1001"));
            assertTrue(signString.contains("type=wxpay"));

            // 验证字典序
            int clientipIndex = signString.indexOf("clientip=");
            int deviceIndex = signString.indexOf("device=");
            int moneyIndex = signString.indexOf("money=");
            int nameIndex = signString.indexOf("name=");

            assertTrue(clientipIndex < deviceIndex, "clientip 应在 device 之前");
            assertTrue(deviceIndex < moneyIndex, "device 应在 money 之前");
            assertTrue(moneyIndex < nameIndex, "money 应在 name 之前");
        }

        @Test
        @DisplayName("构建签名字符串 - 包含 returnUrl")
        void buildSignString_shouldIncludeReturnUrl_whenProvided() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();
            request.setReturnUrl("http://example.com/return");

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            assertTrue(signString.contains("return_url=http://example.com/return"));
        }

        @Test
        @DisplayName("构建签名字符串 - returnUrl 为 null 时不包含")
        void buildSignString_shouldExcludeReturnUrl_whenNull() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();
            request.setReturnUrl(null);

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            assertFalse(signString.contains("return_url"));
        }

        @Test
        @DisplayName("构建签名字符串 - 包含时间戳")
        void buildSignString_shouldIncludeTimestamp_whenProvided() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();
            request.setTimestamp(1700000000L);

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            assertTrue(signString.contains("timestamp=1700000000"));
        }

        @Test
        @DisplayName("构建签名字符串 - 包含 nonce")
        void buildSignString_shouldIncludeNonce_whenProvided() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();
            request.setNonce("abc123xyz");

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            assertTrue(signString.contains("nonce=abc123xyz"));
        }

        @Test
        @DisplayName("构建签名字符串 - 包含 attach 扩展字段")
        void buildSignString_shouldIncludeAttachFields() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();
            Map<String, Object> attach = new HashMap<>();
            attach.put("extra1", "value1");
            attach.put("extra2", "value2");
            request.setAttach(attach);

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            assertTrue(signString.contains("extra1=value1"));
            assertTrue(signString.contains("extra2=value2"));
        }

        @Test
        @DisplayName("构建签名字符串 - 排除 sign 字段")
        void buildSignString_shouldExcludeSignField() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();
            request.setSign("existingSignature");
            Map<String, Object> attach = new HashMap<>();
            attach.put("sign", "anotherSign");
            attach.put("SIGN", "upperCaseSign");
            request.setAttach(attach);

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            // sign 字段不应出现在签名字符串中（不区分大小写）
            assertFalse(signString.contains("sign=existingSignature"));
            assertFalse(signString.contains("sign=anotherSign"));
            assertFalse(signString.contains("SIGN=upperCaseSign"));
        }

        @Test
        @DisplayName("构建签名字符串 - 字段用 & 连接")
        void buildSignString_shouldJoinWithAmpersand() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();

            // When
            String signString = SignatureUtils.buildSignString(request);

            // Then
            String[] parts = signString.split("&");
            assertTrue(parts.length > 1, "应该有多个字段用 & 连接");

            // 结尾不应有 &
            assertFalse(signString.endsWith("&"));
        }
    }

    @Nested
    @DisplayName("buildSignString(Map) 测试")
    class BuildSignStringFromMapTests {

        @Test
        @DisplayName("从 Map 构建签名字符串 - 按字典序排列")
        void buildSignString_shouldSortMapKeysAlphabetically() {
            // Given
            Map<String, Object> data = new HashMap<>();
            data.put("zebra", "last");
            data.put("apple", "first");
            data.put("mango", "middle");

            // When
            String signString = SignatureUtils.buildSignString(data);

            // Then
            assertEquals("apple=first&mango=middle&zebra=last", signString);
        }

        @Test
        @DisplayName("从 Map 构建签名字符串 - 过滤 null 值")
        void buildSignString_shouldFilterNullValues() {
            // Given
            Map<String, Object> data = new HashMap<>();
            data.put("key1", "value1");
            data.put("key2", null);
            data.put("key3", "value3");

            // When
            String signString = SignatureUtils.buildSignString(data);

            // Then
            assertTrue(signString.contains("key1=value1"));
            assertFalse(signString.contains("key2"));
            assertTrue(signString.contains("key3=value3"));
        }

        @Test
        @DisplayName("从 Map 构建签名字符串 - 排除 sign 字段（不区分大小写）")
        void buildSignString_shouldExcludeSignFieldCaseInsensitive() {
            // Given
            Map<String, Object> data = new HashMap<>();
            data.put("amount", "100");
            data.put("sign", "shouldBeExcluded");
            data.put("SIGN", "alsoExcluded");
            data.put("Sign", "stillExcluded");

            // When
            String signString = SignatureUtils.buildSignString(data);

            // Then
            assertEquals("amount=100", signString);
        }

        @Test
        @DisplayName("从 Map 构建签名字符串 - 空 Map")
        void buildSignString_shouldReturnEmptyString_whenMapIsEmpty() {
            // Given
            Map<String, Object> data = new HashMap<>();

            // When
            String signString = SignatureUtils.buildSignString(data);

            // Then
            assertEquals("", signString);
        }

        @Test
        @DisplayName("从 Map 构建签名字符串 - 处理数字类型")
        void buildSignString_shouldHandleNumericValues() {
            // Given
            Map<String, Object> data = new HashMap<>();
            data.put("intValue", 100);
            data.put("longValue", 1234567890L);
            data.put("doubleValue", 99.99);

            // When
            String signString = SignatureUtils.buildSignString(data);

            // Then
            assertTrue(signString.contains("intValue=100"));
            assertTrue(signString.contains("longValue=1234567890"));
            assertTrue(signString.contains("doubleValue=99.99"));
        }
    }

    @Nested
    @DisplayName("md5() 测试")
    class Md5Tests {

        @Test
        @DisplayName("MD5 哈希 - 返回 32 位小写十六进制")
        void md5_shouldReturn32CharLowercaseHex() {
            // Given
            String content = "test string";

            // When
            String hash = SignatureUtils.md5(content);

            // Then
            assertEquals(32, hash.length());
            assertTrue(hash.matches("[0-9a-f]{32}"), "应该只包含小写十六进制字符");
        }

        @Test
        @DisplayName("MD5 哈希 - 相同输入产生相同输出")
        void md5_shouldReturnSameHashForSameInput() {
            // Given
            String content = "consistent input";

            // When
            String hash1 = SignatureUtils.md5(content);
            String hash2 = SignatureUtils.md5(content);

            // Then
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("MD5 哈希 - 不同输入产生不同输出")
        void md5_shouldReturnDifferentHashForDifferentInput() {
            // When
            String hash1 = SignatureUtils.md5("input1");
            String hash2 = SignatureUtils.md5("input2");

            // Then
            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("MD5 哈希 - 已知值验证")
        void md5_shouldMatchKnownValue() {
            // Given - 已知 MD5 值
            // MD5("hello") = 5d41402abc4b2a76b9719d911017c592
            String content = "hello";

            // When
            String hash = SignatureUtils.md5(content);

            // Then
            assertEquals("5d41402abc4b2a76b9719d911017c592", hash);
        }

        @Test
        @DisplayName("MD5 哈希 - 处理中文字符")
        void md5_shouldHandleChineseCharacters() {
            // Given
            String content = "中文测试";

            // When
            String hash = SignatureUtils.md5(content);

            // Then
            assertEquals(32, hash.length());
            // 使用 UTF-8 编码的中文 MD5
            assertNotNull(hash);
        }

        @Test
        @DisplayName("MD5 哈希 - 处理空字符串")
        void md5_shouldHandleEmptyString() {
            // Given - MD5("") = d41d8cd98f00b204e9800998ecf8427e
            String content = "";

            // When
            String hash = SignatureUtils.md5(content);

            // Then
            assertEquals("d41d8cd98f00b204e9800998ecf8427e", hash);
        }

        @Test
        @DisplayName("MD5 哈希 - 处理特殊字符")
        void md5_shouldHandleSpecialCharacters() {
            // Given
            String content = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

            // When
            String hash = SignatureUtils.md5(content);

            // Then
            assertEquals(32, hash.length());
        }
    }

    @Nested
    @DisplayName("完整签名流程测试")
    class FullSignatureFlowTests {

        @Test
        @DisplayName("完整签名流程 - 构建签名并验证")
        void fullSignatureFlow_shouldGenerateValidSignature() {
            // Given
            PublicCreateOrderDTO request = createBasicRequest();
            String secret = "testSecretKey123";

            // When
            String signString = SignatureUtils.buildSignString(request);
            String signature = SignatureUtils.md5(signString + secret);

            // Then
            assertNotNull(signature);
            assertEquals(32, signature.length());

            // 验证相同参数生成相同签名
            String signString2 = SignatureUtils.buildSignString(request);
            String signature2 = SignatureUtils.md5(signString2 + secret);
            assertEquals(signature, signature2);
        }

        @Test
        @DisplayName("完整签名流程 - 参数顺序不影响签名")
        void fullSignatureFlow_signatureShouldBeConsistentRegardlessOfInsertionOrder() {
            // Given - 两个相同内容但插入顺序不同的 Map
            Map<String, Object> data1 = new HashMap<>();
            data1.put("a", "1");
            data1.put("b", "2");
            data1.put("c", "3");

            Map<String, Object> data2 = new HashMap<>();
            data2.put("c", "3");
            data2.put("a", "1");
            data2.put("b", "2");

            String secret = "key";

            // When
            String sign1 = SignatureUtils.md5(SignatureUtils.buildSignString(data1) + secret);
            String sign2 = SignatureUtils.md5(SignatureUtils.buildSignString(data2) + secret);

            // Then
            assertEquals(sign1, sign2, "相同参数不同插入顺序应产生相同签名");
        }
    }

    // ==================== 辅助方法 ====================

    private PublicCreateOrderDTO createBasicRequest() {
        PublicCreateOrderDTO request = new PublicCreateOrderDTO();
        request.setPid(1001L);
        request.setType("wxpay");
        request.setOutTradeNo("TEST123456");
        request.setName("测试商品");
        request.setMoney(new BigDecimal("100.00"));
        request.setNotifyUrl("http://example.com/notify");
        request.setClientIp("127.0.0.1");
        request.setDevice("pc");
        return request;
    }
}
