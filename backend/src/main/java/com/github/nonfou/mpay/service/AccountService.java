package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.account.AccountCreateRequest;
import com.github.nonfou.mpay.dto.account.AccountSummary;
import com.github.nonfou.mpay.dto.account.AccountTransactionDTO;
import com.github.nonfou.mpay.dto.account.ChannelCreateRequest;
import com.github.nonfou.mpay.dto.account.ChannelSummary;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountService {

    PageResponse<AccountSummary> listAccounts(Long pid, String platform, Integer state, Integer pattern, int page,
            int pageSize);

    AccountSummary createAccount(Long pid, AccountCreateRequest request);

    void updateAccount(Long pid, AccountCreateRequest request);

    void updateAccountState(Long accountId, Integer state);

    void deleteAccounts(List<Long> accountIds);

    void addChannel(Long accountId, ChannelCreateRequest request);

    List<ChannelSummary> listChannels(Long accountId);

    void deleteChannel(Long channelId);

    // P1: 账号交易流水查询
    List<AccountTransactionDTO> getAccountTransactions(Long accountId, LocalDateTime startTime, LocalDateTime endTime);
}
