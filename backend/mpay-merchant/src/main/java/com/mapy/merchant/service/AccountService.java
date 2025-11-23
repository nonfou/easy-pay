package com.mapy.merchant.service;

import com.mapy.common.response.PageResponse;
import com.mapy.merchant.dto.AccountCreateRequest;
import com.mapy.merchant.dto.AccountSummary;
import com.mapy.merchant.dto.ChannelCreateRequest;
import java.util.List;

public interface AccountService {

    PageResponse<AccountSummary> listAccounts(Long pid, String platform, Integer state, Integer pattern, int page,
            int pageSize);

    AccountSummary createAccount(Long pid, AccountCreateRequest request);

    void updateAccount(Long pid, AccountCreateRequest request);

    void updateAccountState(Long accountId, Integer state);

    void deleteAccounts(List<Long> accountIds);

    void addChannel(Long accountId, ChannelCreateRequest request);

    List<com.mapy.merchant.dto.ChannelSummary> listChannels(Long accountId);

    void deleteChannel(Long channelId);
}
