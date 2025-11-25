package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.monitor.AccountListenStatusDTO;
import java.util.List;

/**
 * 监听服务 - P2 功能
 */
public interface ListenService {

    /**
     * 获取所有账号的监听状态
     * @param pid 商户ID (可选)
     * @return 账号监听状态列表
     */
    List<AccountListenStatusDTO> getAccountListenStatus(Long pid);

    /**
     * 获取被动监听账号
     * @param pid 商户ID (可选)
     * @return 被动监听账号列表
     */
    List<AccountListenStatusDTO> getPassiveListenAccounts(Long pid);

    /**
     * 获取主动监听账号
     * @param pid 商户ID (可选)
     * @return 主动监听账号列表
     */
    List<AccountListenStatusDTO> getActiveListenAccounts(Long pid);

    /**
     * 更新账号监听模式
     * @param accountId 账号ID
     * @param pattern 监听模式 (0=被动, 1=主动)
     */
    void updateListenPattern(Long accountId, Integer pattern);
}
