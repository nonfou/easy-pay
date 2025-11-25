package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.entity.PayAccountEntity;
import com.github.nonfou.mpay.entity.PayChannelEntity;
import com.github.nonfou.mpay.repository.PayAccountRepository;
import com.github.nonfou.mpay.repository.PayChannelRepository;
import com.github.nonfou.mpay.service.ChannelSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SimpleChannelSelector 单元测试
 * 测试支付通道选择器
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleChannelSelector 通道选择器测试")
class SimpleChannelSelectorTest {

    @Mock
    private PayAccountRepository payAccountRepository;

    @Mock
    private PayChannelRepository payChannelRepository;

    private SimpleChannelSelector channelSelector;

    private static final Long TEST_PID = 1001L;
    private static final String TEST_PAY_TYPE = "wxpay";

    @BeforeEach
    void setUp() {
        channelSelector = new SimpleChannelSelector(payAccountRepository, payChannelRepository);
    }

    @Nested
    @DisplayName("通道选择成功测试")
    class SuccessfulSelectionTests {

        @Test
        @DisplayName("选择通道成功 - 返回有效账号和通道")
        void select_shouldReturnChannelSelection_whenAccountAndChannelExist() {
            // Given
            PayAccountEntity account = createAccount(1L, TEST_PID, 1, 0);
            PayChannelEntity channel = createChannel(10L, account);

            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(List.of(account));
            when(payChannelRepository.findAll()).thenReturn(List.of(channel));

            // When
            Optional<ChannelSelector.ChannelSelection> result = channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertTrue(result.isPresent());
            ChannelSelector.ChannelSelection selection = result.get();
            assertEquals(1L, selection.aid());
            assertEquals(10L, selection.cid());
            assertEquals(0, selection.pattern());
        }

        @Test
        @DisplayName("选择通道成功 - 更新通道最后使用时间")
        void select_shouldUpdateLastTime_whenChannelSelected() {
            // Given
            PayAccountEntity account = createAccount(1L, TEST_PID, 1, 0);
            PayChannelEntity channel = createChannel(10L, account);
            assertNull(channel.getLastTime());

            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(List.of(account));
            when(payChannelRepository.findAll()).thenReturn(List.of(channel));

            // When
            channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertNotNull(channel.getLastTime());
        }

        @Test
        @DisplayName("选择通道成功 - 多个账号选择第一个")
        void select_shouldSelectFirstAccount_whenMultipleAccountsExist() {
            // Given
            PayAccountEntity account1 = createAccount(1L, TEST_PID, 1, 0);
            PayAccountEntity account2 = createAccount(2L, TEST_PID, 1, 1);
            PayChannelEntity channel1 = createChannel(10L, account1);
            PayChannelEntity channel2 = createChannel(20L, account2);

            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(List.of(account1, account2));
            when(payChannelRepository.findAll()).thenReturn(List.of(channel1, channel2));

            // When
            Optional<ChannelSelector.ChannelSelection> result = channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertTrue(result.isPresent());
            assertEquals(1L, result.get().aid());
            assertEquals(10L, result.get().cid());
        }

        @Test
        @DisplayName("选择通道成功 - 返回账号的监听模式")
        void select_shouldReturnAccountPattern() {
            // Given
            PayAccountEntity account = createAccount(1L, TEST_PID, 1, 1); // pattern = 1 (主动监听)
            PayChannelEntity channel = createChannel(10L, account);

            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(List.of(account));
            when(payChannelRepository.findAll()).thenReturn(List.of(channel));

            // When
            Optional<ChannelSelector.ChannelSelection> result = channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertTrue(result.isPresent());
            assertEquals(1, result.get().pattern());
        }
    }

    @Nested
    @DisplayName("通道选择失败测试")
    class FailedSelectionTests {

        @Test
        @DisplayName("选择失败 - 无可用账号")
        void select_shouldReturnEmpty_whenNoAccountExists() {
            // Given
            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(Collections.emptyList());

            // When
            Optional<ChannelSelector.ChannelSelection> result = channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertTrue(result.isEmpty());
            verify(payChannelRepository, never()).findAll();
        }

        @Test
        @DisplayName("选择失败 - 账号无关联通道")
        void select_shouldReturnEmpty_whenNoChannelForAccount() {
            // Given
            PayAccountEntity account = createAccount(1L, TEST_PID, 1, 0);
            PayAccountEntity otherAccount = createAccount(99L, 9999L, 1, 0);
            PayChannelEntity channelForOther = createChannel(10L, otherAccount);

            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(List.of(account));
            when(payChannelRepository.findAll()).thenReturn(List.of(channelForOther));

            // When
            Optional<ChannelSelector.ChannelSelection> result = channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("选择失败 - 通道列表为空")
        void select_shouldReturnEmpty_whenNoChannelsExist() {
            // Given
            PayAccountEntity account = createAccount(1L, TEST_PID, 1, 0);

            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(List.of(account));
            when(payChannelRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            Optional<ChannelSelector.ChannelSelection> result = channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("只查询启用状态的账号")
        void select_shouldOnlyQueryEnabledAccounts() {
            // Given
            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(Collections.emptyList());

            // When
            channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            verify(payAccountRepository).findByPidAndState(TEST_PID, 1); // state = 1 表示启用
            verify(payAccountRepository, never()).findByPidAndState(eq(TEST_PID), eq(0));
        }

        @Test
        @DisplayName("不同商户选择各自的通道")
        void select_shouldSelectChannelsForCorrectMerchant() {
            // Given
            Long pid1 = 1001L;
            Long pid2 = 1002L;

            PayAccountEntity account1 = createAccount(1L, pid1, 1, 0);
            PayAccountEntity account2 = createAccount(2L, pid2, 1, 0);
            PayChannelEntity channel1 = createChannel(10L, account1);
            PayChannelEntity channel2 = createChannel(20L, account2);

            when(payAccountRepository.findByPidAndState(pid1, 1)).thenReturn(List.of(account1));
            when(payAccountRepository.findByPidAndState(pid2, 1)).thenReturn(List.of(account2));
            when(payChannelRepository.findAll()).thenReturn(List.of(channel1, channel2));

            // When
            Optional<ChannelSelector.ChannelSelection> result1 = channelSelector.select(pid1, TEST_PAY_TYPE);
            Optional<ChannelSelector.ChannelSelection> result2 = channelSelector.select(pid2, TEST_PAY_TYPE);

            // Then
            assertTrue(result1.isPresent());
            assertEquals(1L, result1.get().aid());
            assertEquals(10L, result1.get().cid());

            assertTrue(result2.isPresent());
            assertEquals(2L, result2.get().aid());
            assertEquals(20L, result2.get().cid());
        }

        @Test
        @DisplayName("多个通道选择第一个匹配的")
        void select_shouldSelectFirstMatchingChannel() {
            // Given
            PayAccountEntity account = createAccount(1L, TEST_PID, 1, 0);
            PayChannelEntity channel1 = createChannel(10L, account);
            PayChannelEntity channel2 = createChannel(20L, account);
            PayChannelEntity channel3 = createChannel(30L, account);

            when(payAccountRepository.findByPidAndState(TEST_PID, 1)).thenReturn(List.of(account));
            when(payChannelRepository.findAll()).thenReturn(List.of(channel1, channel2, channel3));

            // When
            Optional<ChannelSelector.ChannelSelection> result = channelSelector.select(TEST_PID, TEST_PAY_TYPE);

            // Then
            assertTrue(result.isPresent());
            assertEquals(10L, result.get().cid());
        }
    }

    // ==================== 辅助方法 ====================

    private PayAccountEntity createAccount(Long id, Long pid, Integer state, Integer pattern) {
        PayAccountEntity account = new PayAccountEntity();
        account.setId(id);
        account.setPid(pid);
        account.setState(state);
        account.setPattern(pattern);
        account.setPlatform("wxpay");
        account.setAccount("testAccount");
        account.setPassword("testPassword");
        return account;
    }

    private PayChannelEntity createChannel(Long id, PayAccountEntity account) {
        PayChannelEntity channel = new PayChannelEntity();
        channel.setId(id);
        channel.setAccount(account);
        channel.setChannel("personal");
        channel.setState(1);
        return channel;
    }
}
