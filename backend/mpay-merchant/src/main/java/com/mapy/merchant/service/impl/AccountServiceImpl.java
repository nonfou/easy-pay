package com.mapy.merchant.service.impl;

import com.mapy.common.error.BusinessException;
import com.mapy.common.error.ErrorCode;
import com.mapy.common.response.PageResponse;
import com.mapy.merchant.dto.AccountCreateRequest;
import com.mapy.merchant.dto.AccountSummary;
import com.mapy.merchant.dto.ChannelCreateRequest;
import com.mapy.merchant.dto.ChannelSummary;
import com.mapy.merchant.entity.PayAccountEntity;
import com.mapy.merchant.entity.PayChannelEntity;
import com.mapy.merchant.repository.PayAccountRepository;
import com.mapy.merchant.repository.PayChannelRepository;
import com.mapy.merchant.service.AccountService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private final PayAccountRepository accountRepository;
    private final PayChannelRepository channelRepository;

    public AccountServiceImpl(PayAccountRepository accountRepository,
            PayChannelRepository channelRepository) {
        this.accountRepository = accountRepository;
        this.channelRepository = channelRepository;
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
}
