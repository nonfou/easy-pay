package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.dto.monitor.AccountListenStatusDTO;
import com.github.nonfou.mpay.dto.monitor.ListenPattern;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.PayAccountEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.repository.PayAccountRepository;
import com.github.nonfou.mpay.service.impl.ListenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ListenService 单元测���
 * 测试监听服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListenService 监听服务测试")
class ListenServiceTest {

    @Mock
    private PayAccountRepository accountRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ListenServiceImpl listenService;

    @BeforeEach
    void setUp() {
        listenService = new ListenServiceImpl(accountRepository, orderRepository, redisTemplate);
    }

    @Nested
    @DisplayName("获取账号监听状态测试")
    class GetAccountListenStatusTests {

        @Test
        @DisplayName("获取账号监听状态 - 有账号且在线")
        void getAccountListenStatus_shouldReturnOnlineStatus_whenHeartbeatExists() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", 1);

            when(accountRepository.findActiveAccounts(1001L)).thenReturn(List.of(account));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("mpay:heartbeat:1")).thenReturn("2024-11-25 10:00:00");
            when(orderRepository.findActiveOrders(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AccountListenStatusDTO> result = listenService.getAccountListenStatus(1001L);

            // Then
            assertEquals(1, result.size());
            AccountListenStatusDTO status = result.get(0);
            assertEquals(1L, status.getAccountId());
            assertEquals("wxpay", status.getPlatform());
            assertEquals("acc1", status.getAccount());
            assertTrue(status.getOnline());
            assertEquals("2024-11-25 10:00:00", status.getLastHeartbeat());
        }

        @Test
        @DisplayName("获取账号监听状态 - 有账号但离线")
        void getAccountListenStatus_shouldReturnOfflineStatus_whenNoHeartbeat() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", 0);

            when(accountRepository.findActiveAccounts(1001L)).thenReturn(List.of(account));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("mpay:heartbeat:1")).thenReturn(null);
            when(orderRepository.findActiveOrders(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AccountListenStatusDTO> result = listenService.getAccountListenStatus(1001L);

            // Then
            assertEquals(1, result.size());
            assertFalse(result.get(0).getOnline());
        }

        @Test
        @DisplayName("获取账号监听状态 - 无账号")
        void getAccountListenStatus_shouldReturnEmptyList_whenNoAccounts() {
            // Given
            when(accountRepository.findActiveAccounts(1001L)).thenReturn(Collections.emptyList());

            // When
            List<AccountListenStatusDTO> result = listenService.getAccountListenStatus(1001L);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取账号监听状态 - 包含活跃订单数")
        void getAccountListenStatus_shouldIncludeActiveOrderCount() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", 1);

            when(accountRepository.findActiveAccounts(1001L)).thenReturn(List.of(account));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(orderRepository.findActiveOrders(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(List.of(new OrderEntity(), new OrderEntity(), new OrderEntity()));

            // When
            List<AccountListenStatusDTO> result = listenService.getAccountListenStatus(1001L);

            // Then
            assertEquals(3, result.get(0).getActiveOrderCount());
        }
    }

    @Nested
    @DisplayName("获取被动监听账号测试")
    class GetPassiveListenAccountsTests {

        @Test
        @DisplayName("获取被动监听账号列表")
        void getPassiveListenAccounts_shouldReturnPassiveAccounts() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", ListenPattern.PASSIVE.getCode());

            when(accountRepository.findActiveByPattern(1001L, ListenPattern.PASSIVE.getCode()))
                    .thenReturn(List.of(account));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(orderRepository.findActiveOrders(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AccountListenStatusDTO> result = listenService.getPassiveListenAccounts(1001L);

            // Then
            assertEquals(1, result.size());
            assertEquals(ListenPattern.PASSIVE.getCode(), result.get(0).getPattern());
            assertEquals("被动监听", result.get(0).getPatternName());
        }
    }

    @Nested
    @DisplayName("获取主动监听账号测试")
    class GetActiveListenAccountsTests {

        @Test
        @DisplayName("获取主动监听账号列表")
        void getActiveListenAccounts_shouldReturnActiveAccounts() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "alipay", "acc2", ListenPattern.ACTIVE.getCode());

            when(accountRepository.findActiveByPattern(1001L, ListenPattern.ACTIVE.getCode()))
                    .thenReturn(List.of(account));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(orderRepository.findActiveOrders(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AccountListenStatusDTO> result = listenService.getActiveListenAccounts(1001L);

            // Then
            assertEquals(1, result.size());
            assertEquals(ListenPattern.ACTIVE.getCode(), result.get(0).getPattern());
            assertEquals("主动监听", result.get(0).getPatternName());
        }
    }

    @Nested
    @DisplayName("更新监听模式测试")
    class UpdateListenPatternTests {

        @Test
        @DisplayName("更新监听模式 - 从被动改为主动")
        void updateListenPattern_shouldUpdateToActive() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", ListenPattern.PASSIVE.getCode());

            when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(PayAccountEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            listenService.updateListenPattern(1L, ListenPattern.ACTIVE.getCode());

            // Then
            ArgumentCaptor<PayAccountEntity> captor = ArgumentCaptor.forClass(PayAccountEntity.class);
            verify(accountRepository).save(captor.capture());

            assertEquals(ListenPattern.ACTIVE.getCode(), captor.getValue().getPattern());
        }

        @Test
        @DisplayName("更新监听模式 - 从主动改为被动")
        void updateListenPattern_shouldUpdateToPassive() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", ListenPattern.ACTIVE.getCode());

            when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(PayAccountEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            listenService.updateListenPattern(1L, ListenPattern.PASSIVE.getCode());

            // Then
            ArgumentCaptor<PayAccountEntity> captor = ArgumentCaptor.forClass(PayAccountEntity.class);
            verify(accountRepository).save(captor.capture());

            assertEquals(ListenPattern.PASSIVE.getCode(), captor.getValue().getPattern());
        }

        @Test
        @DisplayName("更新监听模式失败 - 无效的模式值(负数)")
        void updateListenPattern_shouldThrowException_whenPatternIsNegative() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> listenService.updateListenPattern(1L, -1));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("无效的监听模式"));

            verify(accountRepository, never()).findById(anyLong());
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("更新监听模式失败 - 无效的模式值(大于1)")
        void updateListenPattern_shouldThrowException_whenPatternIsTooLarge() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> listenService.updateListenPattern(1L, 2));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("更新监听模式失败 - 账号不存在")
        void updateListenPattern_shouldThrowException_whenAccountNotFound() {
            // Given
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> listenService.updateListenPattern(999L, 0));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("账号不存在"));

            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("监听模式枚举转换测试")
    class ListenPatternConversionTests {

        @Test
        @DisplayName("被动监听模式信息正确")
        void passivePattern_shouldHaveCorrectInfo() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", ListenPattern.PASSIVE.getCode());

            when(accountRepository.findActiveAccounts(1001L)).thenReturn(List.of(account));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(orderRepository.findActiveOrders(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AccountListenStatusDTO> result = listenService.getAccountListenStatus(1001L);

            // Then
            AccountListenStatusDTO status = result.get(0);
            assertEquals(0, status.getPattern());
            assertEquals("被动监听", status.getPatternName());
            assertEquals("等待平台推送支付通知", status.getPatternDescription());
        }

        @Test
        @DisplayName("主动监听模式信息正确")
        void activePattern_shouldHaveCorrectInfo() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1", ListenPattern.ACTIVE.getCode());

            when(accountRepository.findActiveAccounts(1001L)).thenReturn(List.of(account));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(orderRepository.findActiveOrders(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AccountListenStatusDTO> result = listenService.getAccountListenStatus(1001L);

            // Then
            AccountListenStatusDTO status = result.get(0);
            assertEquals(1, status.getPattern());
            assertEquals("主动监听", status.getPatternName());
            assertEquals("主动查询平台支付记录", status.getPatternDescription());
        }
    }

    // ==================== 辅助方法 ====================

    private PayAccountEntity createAccount(Long id, Long pid, String platform, String account, Integer pattern) {
        PayAccountEntity entity = new PayAccountEntity();
        entity.setId(id);
        entity.setPid(pid);
        entity.setPlatform(platform);
        entity.setAccount(account);
        entity.setPattern(pattern);
        entity.setState(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
