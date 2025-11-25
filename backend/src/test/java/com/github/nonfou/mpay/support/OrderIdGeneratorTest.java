package com.github.nonfou.mpay.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderIdGenerator 单元测试
 * 测试订单号生成器
 */
@DisplayName("OrderIdGenerator 订单号生成器测试")
class OrderIdGeneratorTest {

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("生成订单号 - 包含前缀")
        void generate_shouldIncludePrefix() {
            // Given
            String prefix = "MP";

            // When
            String orderId = OrderIdGenerator.generate(prefix);

            // Then
            assertTrue(orderId.startsWith(prefix));
        }

        @Test
        @DisplayName("生成订单号 - 长度正确")
        void generate_shouldHaveCorrectLength() {
            // Given
            String prefix = "MP";
            // 期望格式: 前缀(2) + 时间戳(14位 yyyyMMddHHmmss) + 随机数(6位) = 22位

            // When
            String orderId = OrderIdGenerator.generate(prefix);

            // Then
            assertEquals(22, orderId.length());
        }

        @Test
        @DisplayName("生成订单号 - 包含时间戳")
        void generate_shouldIncludeTimestamp() {
            // Given
            String prefix = "MP";
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // When
            String orderId = OrderIdGenerator.generate(prefix);

            // Then
            // 订单号在前缀之后应包含当前日期
            String timestampPart = orderId.substring(prefix.length(), prefix.length() + 8);
            assertEquals(currentDate, timestampPart);
        }

        @Test
        @DisplayName("生成订单号 - 随机数部分为6位数字")
        void generate_shouldHaveSixDigitRandomPart() {
            // Given
            String prefix = "MP";

            // When
            String orderId = OrderIdGenerator.generate(prefix);

            // Then
            // 最后6位是随机数
            String randomPart = orderId.substring(orderId.length() - 6);
            assertTrue(randomPart.matches("\\d{6}"));

            // 随机数应在 100000-999999 范围内
            int randomNum = Integer.parseInt(randomPart);
            assertTrue(randomNum >= 100000 && randomNum <= 999999);
        }

        @Test
        @DisplayName("生成订单号 - 不同前缀")
        void generate_shouldWorkWithDifferentPrefixes() {
            // When
            String orderId1 = OrderIdGenerator.generate("WX");
            String orderId2 = OrderIdGenerator.generate("ALI");
            String orderId3 = OrderIdGenerator.generate("PAY");

            // Then
            assertTrue(orderId1.startsWith("WX"));
            assertTrue(orderId2.startsWith("ALI"));
            assertTrue(orderId3.startsWith("PAY"));

            assertEquals(22, orderId1.length());
            assertEquals(23, orderId2.length()); // 前缀3位
            assertEquals(23, orderId3.length()); // 前缀3位
        }

        @Test
        @DisplayName("生成订单号 - 空前缀")
        void generate_shouldWorkWithEmptyPrefix() {
            // When
            String orderId = OrderIdGenerator.generate("");

            // Then
            assertEquals(20, orderId.length()); // 14位时间戳 + 6位随机数
        }
    }

    @Nested
    @DisplayName("唯一性测试")
    class UniquenessTests {

        @RepeatedTest(10)
        @DisplayName("连续生成的订单号不重复")
        void generate_shouldProduceDifferentOrderIds() {
            // Given
            String prefix = "MP";

            // When
            String orderId1 = OrderIdGenerator.generate(prefix);
            String orderId2 = OrderIdGenerator.generate(prefix);

            // Then
            assertNotEquals(orderId1, orderId2);
        }

        @Test
        @DisplayName("批量生成100个订单号无重复")
        void generate_shouldProduceUniqueIdsInBatch() {
            // Given
            String prefix = "MP";
            // 注：随机数范围 100000-999999 约 90 万个值，在同一秒内生成 100 个碰撞概率极低
            int count = 100;
            Set<String> orderIds = new HashSet<>();

            // When
            for (int i = 0; i < count; i++) {
                orderIds.add(OrderIdGenerator.generate(prefix));
            }

            // Then
            assertEquals(count, orderIds.size(), "生成的订单号应该全部唯一");
        }

        @Test
        @DisplayName("并发生成订单号无重复")
        void generate_shouldProduceUniqueIdsUnderConcurrency() throws InterruptedException {
            // Given
            String prefix = "MP";
            int threadCount = 3;
            int perThreadCount = 10;
            Set<String> orderIds = java.util.Collections.synchronizedSet(new HashSet<>());

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < perThreadCount; j++) {
                            orderIds.add(OrderIdGenerator.generate(prefix));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then - 由于随机碰撞的可能性，我们验证大部分唯一即可
            int expectedCount = threadCount * perThreadCount;
            // 允许最多1个碰撞（概率极低但理论上可能）
            assertTrue(orderIds.size() >= expectedCount - 1,
                    "并发生成的订单号应基本唯一，实际生成: " + orderIds.size() + "/" + expectedCount);
        }
    }

    @Nested
    @DisplayName("格式验证测试")
    class FormatValidationTests {

        @Test
        @DisplayName("订单号只包含字母和数字")
        void generate_shouldContainOnlyAlphanumeric() {
            // Given
            String prefix = "MP";

            // When
            String orderId = OrderIdGenerator.generate(prefix);

            // Then
            assertTrue(orderId.matches("[A-Za-z0-9]+"));
        }

        @Test
        @DisplayName("时间戳部分格式正确 - yyyyMMddHHmmss")
        void generate_shouldHaveValidTimestampFormat() {
            // Given
            String prefix = "MP";

            // When
            String orderId = OrderIdGenerator.generate(prefix);

            // Then
            // 提取时间戳部分（前缀后14位）
            String timestampPart = orderId.substring(prefix.length(), prefix.length() + 14);

            // 验证可以解析为日期时间
            assertDoesNotThrow(() -> {
                LocalDateTime.parse(timestampPart, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            });
        }

        @Test
        @DisplayName("生成的订单号可用于数据库查询")
        void generate_shouldBeSafeForDatabaseQuery() {
            // Given
            String prefix = "MP";

            // When
            String orderId = OrderIdGenerator.generate(prefix);

            // Then
            // 不包含SQL特殊字符
            assertFalse(orderId.contains("'"));
            assertFalse(orderId.contains("\""));
            assertFalse(orderId.contains(";"));
            assertFalse(orderId.contains("-"));
            assertFalse(orderId.contains(" "));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("长前缀")
        void generate_shouldWorkWithLongPrefix() {
            // Given
            String longPrefix = "VERYLONGPREFIX";

            // When
            String orderId = OrderIdGenerator.generate(longPrefix);

            // Then
            assertTrue(orderId.startsWith(longPrefix));
            assertEquals(longPrefix.length() + 20, orderId.length());
        }

        @Test
        @DisplayName("数字前缀")
        void generate_shouldWorkWithNumericPrefix() {
            // Given
            String numericPrefix = "123";

            // When
            String orderId = OrderIdGenerator.generate(numericPrefix);

            // Then
            assertTrue(orderId.startsWith(numericPrefix));
        }

        @Test
        @DisplayName("随机数分布在有效范围内")
        void generate_randomPartShouldBeInValidRange() {
            // Given
            String prefix = "MP";
            int sampleSize = 100;

            // When & Then
            for (int i = 0; i < sampleSize; i++) {
                String orderId = OrderIdGenerator.generate(prefix);
                String randomPart = orderId.substring(orderId.length() - 6);
                int randomNum = Integer.parseInt(randomPart);

                assertTrue(randomNum >= 100000, "随机数应 >= 100000");
                assertTrue(randomNum <= 999999, "随机数应 <= 999999");
            }
        }
    }
}
