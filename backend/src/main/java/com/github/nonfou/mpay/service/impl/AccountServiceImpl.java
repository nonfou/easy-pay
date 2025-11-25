package com.github.nonfou.mpay.service.impl;

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
import com.github.nonfou.mpay.service.AccountService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Map<Integer, String> STATE_NAMES = Map.of(
            0, "待支付",
            1, "已支付",
            2, "已关闭"
    );

    private final PayAccountRepository accountRepository;
    private final PayChannelRepository channelRepository;
    private final OrderRepository orderRepository;

    public AccountServiceImpl(PayAccountRepository accountRepository,
            PayChannelRepository channelRepository,
            OrderRepository orderRepository) {
        this.accountRepository = accountRepository;
        this.channelRepository = channelRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public PageResponse<AccountSummary> listAccounts(Long pid, String platform, Integer state, Integer pattern, int page,
            int pageSize) {
        List<PayAccountEntity> accounts = accountRepository.findByPid(pid).stream()
                .filter(acc -> platform == null || acc.getPlatform().contains(platform))
                .filter(acc -> state == null || acc.getState().equals(state))
                .filter(acc -> pattern == null || acc.getPattern().equals(pattern))
                .collect(Collectors.toList());
        int from = Math.max((page - 1) * pageSize, 0);
        int to = Math.min(from + pageSize, accounts.size());
        List<PayAccountEntity> pageData = from >= accounts.size() ? List.of() : accounts.subList(from, to);
        List<AccountSummary> summaries = pageData.stream()
                .map(entity -> AccountSummary.builder()
                        .id(entity.getId())
                        .pid(entity.getPid())
                        .platform(entity.getPlatform())
                        .account(entity.getAccount())
                        .state(entity.getState())
                        .pattern(entity.getPattern())
                        .channelCount(channelRepository.findByAccountId(entity.getId()).size())
                        .build())
                .collect(Collectors.toList());
        return PageResponse.of(page, pageSize, accounts.size(), summaries);
    }

    @Override
    @Transactional
    public AccountSummary createAccount(Long pid, AccountCreateRequest request) {
        PayAccountEntity entity = new PayAccountEntity();
        entity.setPid(pid);
        entity.setPlatform(request.getPlatform());
        entity.setAccount(request.getAccount());
        entity.setPassword(request.getPassword());
        entity.setPattern(request.getPattern());
        entity.setState(1);
        entity.setParams(request.getParams() == null ? "{}" : request.getParams().toString());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        PayAccountEntity saved = accountRepository.save(entity);
        return AccountSummary.builder()
                .id(saved.getId())
                .pid(saved.getPid())
                .platform(saved.getPlatform())
                .account(saved.getAccount())
                .state(saved.getState())
                .pattern(saved.getPattern())
                .channelCount(0)
                .build();
    }

    @Override
    @Transactional
    public void updateAccount(Long accountId, AccountCreateRequest request) {
        PayAccountEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "account not found"));
        entity.setPlatform(request.getPlatform());
        entity.setAccount(request.getAccount());
        entity.setPassword(request.getPassword());
        entity.setPattern(request.getPattern());
        entity.setParams(request.getParams() == null ? "{}" : request.getParams().toString());
        entity.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateAccountState(Long accountId, Integer state) {
        PayAccountEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "account not found"));
        entity.setState(state);
        accountRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteAccounts(List<Long> accountIds) {
        accountRepository.deleteAllById(accountIds);
    }

    @Override
    @Transactional
    public void addChannel(Long accountId, ChannelCreateRequest request) {
        PayAccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "account not found"));
        PayChannelEntity channel = new PayChannelEntity();
        channel.setAccount(account);
        channel.setChannel(request.getChannel());
        channel.setQrcode(request.getQrcode());
        channel.setType(request.getType());
        channel.setState(1);
        channelRepository.save(channel);
    }

    @Override
    @Transactional
    public void deleteChannel(Long channelId) {
        channelRepository.deleteById(channelId);
    }

    @Override
    public List<ChannelSummary> listChannels(Long accountId) {
        return channelRepository.findByAccountId(accountId).stream()
                .map(channel -> ChannelSummary.builder()
                        .id(channel.getId())
                        .channel(channel.getChannel())
                        .qrcode(channel.getQrcode())
                        .state(channel.getState())
                        .type(channel.getType())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountTransactionDTO> getAccountTransactions(Long accountId, LocalDateTime startTime,
            LocalDateTime endTime) {
        // 验证账号存在
        accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账号不存在: " + accountId));

        // 查询该账号的所有订单
        List<OrderEntity> orders = orderRepository.findByAccountIdAndTimeRange(accountId, startTime, endTime);

        // 获取通道信息用于显示通道名称
        Map<Long, String> channelNames = channelRepository.findByAccountId(accountId).stream()
                .collect(Collectors.toMap(PayChannelEntity::getId, PayChannelEntity::getChannel));

        return orders.stream()
                .map(order -> AccountTransactionDTO.builder()
                        .orderId(order.getOrderId())
                        .outTradeNo(order.getOutTradeNo())
                        .type(order.getType())
                        .name(order.getName())
                        .money(order.getMoney())
                        .reallyPrice(order.getReallyPrice())
                        .state(order.getState())
                        .stateName(STATE_NAMES.getOrDefault(order.getState(), "未知"))
                        .createTime(order.getCreateTime())
                        .payTime(order.getPayTime())
                        .channelName(channelNames.getOrDefault(order.getCid(), "未知"))
                        .build())
                .collect(Collectors.toList());
    }
}
