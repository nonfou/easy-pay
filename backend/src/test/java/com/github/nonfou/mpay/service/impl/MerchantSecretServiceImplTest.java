package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.entity.MerchantEntity;
import com.github.nonfou.mpay.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MerchantSecretService 单元测试
 * 测试商户密钥查询服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantSecretService 商户密钥服务测试")
class MerchantSecretServiceImplTest {

    @Mock
    private MerchantRepository merchantRepository;

    private MerchantSecretServiceImpl merchantSecretService;

    @BeforeEach
    void setUp() {
        merchantSecretService = new MerchantSecretServiceImpl(merchantRepository);
    }

    @Nested
    @DisplayName("获取商户密钥测试")
    class GetSecretTests {

        @Test
        @DisplayName("获取密钥成功 - 商户存在")
        void getSecret_shouldReturnSecret_whenMerchantExists() {
            // Given
            Long pid = 1001L;
            MerchantEntity merchant = new MerchantEntity();
            merchant.setPid(pid);
            merchant.setSecretKey("test-secret-key-12345");

            when(merchantRepository.findByPid(pid)).thenReturn(Optional.of(merchant));

            // When
            Optional<String> result = merchantSecretService.getSecret(pid);

            // Then
            assertTrue(result.isPresent());
            assertEquals("test-secret-key-12345", result.get());
            verify(merchantRepository).findByPid(pid);
        }

        @Test
        @DisplayName("获取密钥失败 - 商户不存在")
        void getSecret_shouldReturnEmpty_whenMerchantNotExists() {
            // Given
            Long pid = 9999L;
            when(merchantRepository.findByPid(pid)).thenReturn(Optional.empty());

            // When
            Optional<String> result = merchantSecretService.getSecret(pid);

            // Then
            assertTrue(result.isEmpty());
            verify(merchantRepository).findByPid(pid);
        }

        @Test
        @DisplayName("获取密钥 - null PID")
        void getSecret_shouldHandleNullPid() {
            // Given
            when(merchantRepository.findByPid(null)).thenReturn(Optional.empty());

            // When
            Optional<String> result = merchantSecretService.getSecret(null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取密钥 - 不同商户返回不同密钥")
        void getSecret_shouldReturnDifferentSecretsForDifferentMerchants() {
            // Given
            Long pid1 = 1001L;
            Long pid2 = 1002L;

            MerchantEntity merchant1 = new MerchantEntity();
            merchant1.setPid(pid1);
            merchant1.setSecretKey("secret-for-merchant-1");

            MerchantEntity merchant2 = new MerchantEntity();
            merchant2.setPid(pid2);
            merchant2.setSecretKey("secret-for-merchant-2");

            when(merchantRepository.findByPid(pid1)).thenReturn(Optional.of(merchant1));
            when(merchantRepository.findByPid(pid2)).thenReturn(Optional.of(merchant2));

            // When
            Optional<String> result1 = merchantSecretService.getSecret(pid1);
            Optional<String> result2 = merchantSecretService.getSecret(pid2);

            // Then
            assertTrue(result1.isPresent());
            assertTrue(result2.isPresent());
            assertNotEquals(result1.get(), result2.get());
            assertEquals("secret-for-merchant-1", result1.get());
            assertEquals("secret-for-merchant-2", result2.get());
        }

        @Test
        @DisplayName("获取密钥 - 密钥可能为空字符串")
        void getSecret_shouldReturnEmptyString_whenSecretIsEmpty() {
            // Given
            Long pid = 1001L;
            MerchantEntity merchant = new MerchantEntity();
            merchant.setPid(pid);
            merchant.setSecretKey("");

            when(merchantRepository.findByPid(pid)).thenReturn(Optional.of(merchant));

            // When
            Optional<String> result = merchantSecretService.getSecret(pid);

            // Then
            assertTrue(result.isPresent());
            assertEquals("", result.get());
        }
    }
}
