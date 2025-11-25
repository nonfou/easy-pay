package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.account.AccountCreateRequest;
import com.github.nonfou.mpay.dto.account.AccountSummary;
import com.github.nonfou.mpay.dto.account.AccountTransactionDTO;
import com.github.nonfou.mpay.dto.account.ChannelCreateRequest;
import com.github.nonfou.mpay.dto.account.ChannelSummary;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.PayAccountEntity;
import com.github.nonfou.mpay.entity.PayChannelEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.repository.PayAccountRepository;
import com.github.nonfou.mpay.repository.PayChannelRepository;
import com.github.nonfou.mpay.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AccountService 单元测试
 * 测试支付账号管理服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 账号管理服务测试")
class AccountServiceTest {

    @Mock
    private PayAccountRepository accountRepository;

    @Mock
    private PayChannelRepository channelRepository;

    @Mock
    private OrderRepository orderRepository;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, channelRepository, orderRepository);
    }

    @Nested
    @DisplayName("账号列表查询测试")
    class ListAccountsTests {

        @Test
        @DisplayName("查询账号列表 - 有数据")
        void listAccounts_shouldReturnPagedResult() {
            // Given
            PayAccountEntity account1 = createAccount(1L, 1001L, "wxpay", "acc1");
            PayAccountEntity account2 = createAccount(2L, 1001L, "alipay", "acc2");
            List<PayAccountEntity> accounts = List.of(account1, account2);
            Page<PayAccountEntity> page = new PageImpl<>(accounts, PageRequest.of(0, 10), 2);

            when(accountRepository.findByConditions(anyLong(), any(), any(), any(), any()))
                    .thenReturn(page);
            when(channelRepository.countChannelsByAccountIds(anyList()))
                    .thenReturn(List.of(new Object[]{1L, 3L}, new Object[]{2L, 2L}));

            // When
            PageResponse<AccountSummary> result = accountService.listAccounts(1001L, null, null, null, 1, 10);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getItems().size());
            assertEquals(2L, result.getTotal());
            assertEquals(1, result.getPage());
            assertEquals(10, result.getPageSize());

            AccountSummary summary1 = result.getItems().get(0);
            assertEquals(1L, summary1.getId());
            assertEquals("wxpay", summary1.getPlatform());
            assertEquals(3, summary1.getChannelCount());
        }

        @Test
        @DisplayName("查询账号列表 - 空结果")
        void listAccounts_shouldReturnEmptyResult_whenNoData() {
            // Given
            Page<PayAccountEntity> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

            when(accountRepository.findByConditions(anyLong(), any(), any(), any(), any()))
                    .thenReturn(emptyPage);

            // When
            PageResponse<AccountSummary> result = accountService.listAccounts(1001L, null, null, null, 1, 10);

            // Then
            assertNotNull(result);
            assertTrue(result.getItems().isEmpty());
            assertEquals(0, result.getTotal());

            verify(channelRepository, never()).countChannelsByAccountIds(anyList());
        }

        @Test
        @DisplayName("查询账号列表 - 带筛选条件")
        void listAccounts_shouldApplyFilters() {
            // Given
            Page<PayAccountEntity> emptyPage = new PageImpl<>(Collections.emptyList());

            when(accountRepository.findByConditions(eq(1001L), eq("wxpay"), eq(1), eq(1), any()))
                    .thenReturn(emptyPage);

            // When
            accountService.listAccounts(1001L, "wxpay", 1, 1, 1, 10);

            // Then
            verify(accountRepository).findByConditions(eq(1001L), eq("wxpay"), eq(1), eq(1), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("创建账号测试")
    class CreateAccountTests {

        @Test
        @DisplayName("创建账号成功")
        void createAccount_shouldSaveAndReturnSummary() {
            // Given
            AccountCreateRequest request = new AccountCreateRequest();
            request.setPlatform("wxpay");
            request.setAccount("test_account");
            request.setPassword("test_password");
            request.setPattern(1);

            when(accountRepository.save(any(PayAccountEntity.class)))
                    .thenAnswer(i -> {
                        PayAccountEntity entity = i.getArgument(0);
                        entity.setId(1L);
                        return entity;
                    });

            // When
            AccountSummary result = accountService.createAccount(1001L, request);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(1001L, result.getPid());
            assertEquals("wxpay", result.getPlatform());
            assertEquals("test_account", result.getAccount());
            assertEquals(1, result.getState()); // 默认启用
            assertEquals(0, result.getChannelCount());

            ArgumentCaptor<PayAccountEntity> captor = ArgumentCaptor.forClass(PayAccountEntity.class);
            verify(accountRepository).save(captor.capture());

            PayAccountEntity saved = captor.getValue();
            assertEquals("test_password", saved.getPassword());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("创建账号 - params为null时使用空对象")
        void createAccount_shouldUseEmptyObjectWhenParamsNull() {
            // Given
            AccountCreateRequest request = new AccountCreateRequest();
            request.setPlatform("wxpay");
            request.setAccount("test_account");
            request.setParams(null);

            when(accountRepository.save(any(PayAccountEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            accountService.createAccount(1001L, request);

            // Then
            ArgumentCaptor<PayAccountEntity> captor = ArgumentCaptor.forClass(PayAccountEntity.class);
            verify(accountRepository).save(captor.capture());

            assertEquals("{}", captor.getValue().getParams());
        }
    }

    @Nested
    @DisplayName("更新账号测试")
    class UpdateAccountTests {

        @Test
        @DisplayName("更新账号成功")
        void updateAccount_shouldUpdateFields() {
            // Given
            PayAccountEntity existing = createAccount(1L, 1001L, "wxpay", "old_account");

            AccountCreateRequest request = new AccountCreateRequest();
            request.setPlatform("alipay");
            request.setAccount("new_account");
            request.setPassword("new_password");
            request.setPattern(2);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any(PayAccountEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            accountService.updateAccount(1L, request);

            // Then
            ArgumentCaptor<PayAccountEntity> captor = ArgumentCaptor.forClass(PayAccountEntity.class);
            verify(accountRepository).save(captor.capture());

            PayAccountEntity saved = captor.getValue();
            assertEquals("alipay", saved.getPlatform());
            assertEquals("new_account", saved.getAccount());
            assertEquals("new_password", saved.getPassword());
            assertEquals(2, saved.getPattern());
            assertNotNull(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("更新账号失败 - 账号不存在")
        void updateAccount_shouldThrowException_whenNotFound() {
            // Given
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            AccountCreateRequest request = new AccountCreateRequest();
            request.setPlatform("wxpay");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> accountService.updateAccount(999L, request));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("更新账号状态测试")
    class UpdateAccountStateTests {

        @Test
        @DisplayName("更新账号状态成功")
        void updateAccountState_shouldUpdateState() {
            // Given
            PayAccountEntity existing = createAccount(1L, 1001L, "wxpay", "acc1");
            existing.setState(1);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any(PayAccountEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            accountService.updateAccountState(1L, 0);

            // Then
            ArgumentCaptor<PayAccountEntity> captor = ArgumentCaptor.forClass(PayAccountEntity.class);
            verify(accountRepository).save(captor.capture());

            assertEquals(0, captor.getValue().getState());
        }

        @Test
        @DisplayName("更新账号状态失败 - 账号不存在")
        void updateAccountState_shouldThrowException_whenNotFound() {
            // Given
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> accountService.updateAccountState(999L, 1));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("删除账号测试")
    class DeleteAccountsTests {

        @Test
        @DisplayName("删除多个账号成功")
        void deleteAccounts_shouldDeleteByIds() {
            // Given
            List<Long> ids = List.of(1L, 2L, 3L);

            // When
            accountService.deleteAccounts(ids);

            // Then
            verify(accountRepository).deleteAllById(ids);
        }
    }

    @Nested
    @DisplayName("通道管理测试")
    class ChannelManagementTests {

        @Test
        @DisplayName("添加通道成功")
        void addChannel_shouldSaveChannel() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1");

            ChannelCreateRequest request = new ChannelCreateRequest();
            request.setChannel("channel_1");
            request.setQrcode("https://example.com/qr.png");
            request.setType("personal");

            when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

            // When
            accountService.addChannel(1L, request);

            // Then
            ArgumentCaptor<PayChannelEntity> captor = ArgumentCaptor.forClass(PayChannelEntity.class);
            verify(channelRepository).save(captor.capture());

            PayChannelEntity saved = captor.getValue();
            assertEquals(account, saved.getAccount());
            assertEquals("channel_1", saved.getChannel());
            assertEquals("https://example.com/qr.png", saved.getQrcode());
            assertEquals("personal", saved.getType());
            assertEquals(1, saved.getState());
        }

        @Test
        @DisplayName("添加通道失败 - 账号不存在")
        void addChannel_shouldThrowException_whenAccountNotFound() {
            // Given
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            ChannelCreateRequest request = new ChannelCreateRequest();
            request.setChannel("channel_1");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> accountService.addChannel(999L, request));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            verify(channelRepository, never()).save(any());
        }

        @Test
        @DisplayName("删除通道成功")
        void deleteChannel_shouldDeleteById() {
            // When
            accountService.deleteChannel(1L);

            // Then
            verify(channelRepository).deleteById(1L);
        }

        @Test
        @DisplayName("查询账号通道列表")
        void listChannels_shouldReturnChannelList() {
            // Given
            PayChannelEntity channel1 = createChannel(1L, "channel_1", "wxpay");
            PayChannelEntity channel2 = createChannel(2L, "channel_2", "alipay");

            when(channelRepository.findByAccountId(1L)).thenReturn(List.of(channel1, channel2));

            // When
            List<ChannelSummary> result = accountService.listChannels(1L);

            // Then
            assertEquals(2, result.size());
            assertEquals("channel_1", result.get(0).getChannel());
            assertEquals("channel_2", result.get(1).getChannel());
        }
    }

    @Nested
    @DisplayName("账号交易记录测试")
    class AccountTransactionsTests {

        @Test
        @DisplayName("查询账号交易记录成功")
        void getAccountTransactions_shouldReturnTransactions() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1");
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();

            OrderEntity order1 = createOrder("H001", 100.00, 0);
            order1.setCid(1L);
            OrderEntity order2 = createOrder("H002", 200.00, 1);
            order2.setCid(2L);

            PayChannelEntity channel1 = createChannel(1L, "channel_1", "wxpay");
            PayChannelEntity channel2 = createChannel(2L, "channel_2", "wxpay");

            when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
            when(orderRepository.findByAccountIdAndTimeRange(1L, startTime, endTime))
                    .thenReturn(List.of(order1, order2));
            when(channelRepository.findByAccountId(1L)).thenReturn(List.of(channel1, channel2));

            // When
            List<AccountTransactionDTO> result = accountService.getAccountTransactions(1L, startTime, endTime);

            // Then
            assertEquals(2, result.size());

            AccountTransactionDTO tx1 = result.get(0);
            assertEquals("H001", tx1.getOrderId());
            assertEquals("待支付", tx1.getStateName());
            assertEquals("channel_1", tx1.getChannelName());

            AccountTransactionDTO tx2 = result.get(1);
            assertEquals("H002", tx2.getOrderId());
            assertEquals("已支付", tx2.getStateName());
        }

        @Test
        @DisplayName("查询账号交易记录失败 - 账号不存在")
        void getAccountTransactions_shouldThrowException_whenAccountNotFound() {
            // Given
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> accountService.getAccountTransactions(999L, LocalDateTime.now(), LocalDateTime.now()));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("查询账号交易记录 - 未知通道显示默认名称")
        void getAccountTransactions_shouldShowUnknownChannel_whenChannelNotFound() {
            // Given
            PayAccountEntity account = createAccount(1L, 1001L, "wxpay", "acc1");
            OrderEntity order = createOrder("H001", 100.00, 0);
            order.setCid(999L); // 不存在的通道ID

            when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
            when(orderRepository.findByAccountIdAndTimeRange(anyLong(), any(), any()))
                    .thenReturn(List.of(order));
            when(channelRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

            // When
            List<AccountTransactionDTO> result = accountService.getAccountTransactions(
                    1L, LocalDateTime.now().minusDays(1), LocalDateTime.now());

            // Then
            assertEquals("未知", result.get(0).getChannelName());
        }
    }

    // ==================== 辅助方法 ====================

    private PayAccountEntity createAccount(Long id, Long pid, String platform, String account) {
        PayAccountEntity entity = new PayAccountEntity();
        entity.setId(id);
        entity.setPid(pid);
        entity.setPlatform(platform);
        entity.setAccount(account);
        entity.setState(1);
        entity.setPattern(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private PayChannelEntity createChannel(Long id, String channel, String type) {
        PayChannelEntity entity = new PayChannelEntity();
        entity.setId(id);
        entity.setChannel(channel);
        entity.setType(type);
        entity.setState(1);
        entity.setQrcode("https://example.com/qr/" + id + ".png");
        return entity;
    }

    private OrderEntity createOrder(String orderId, double money, int state) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setOutTradeNo("OUT" + orderId);
        order.setType("wxpay");
        order.setName("测试商品");
        order.setMoney(new BigDecimal(String.valueOf(money)));
        order.setReallyPrice(new BigDecimal(String.valueOf(money)));
        order.setState(state);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
