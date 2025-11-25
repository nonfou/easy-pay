package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.dto.monitor.AccountListenStatusDTO;
import com.github.nonfou.mpay.service.ListenService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监听管理接口 - P2 功能
 */
@RestController
@RequestMapping("/api/listen")
public class ListenController {

    private final ListenService listenService;

    public ListenController(ListenService listenService) {
        this.listenService = listenService;
    }

    /**
     * 获取所有账号的监听状态
     * GET /api/listen/status
     */
    @GetMapping("/status")
    public ApiResponse<List<AccountListenStatusDTO>> getListenStatus(
            @RequestParam(required = false) Long pid) {
        return ApiResponse.success(listenService.getAccountListenStatus(pid));
    }

    /**
     * 获取被动监听账号
     * GET /api/listen/passive
     */
    @GetMapping("/passive")
    public ApiResponse<List<AccountListenStatusDTO>> getPassiveAccounts(
            @RequestParam(required = false) Long pid) {
        return ApiResponse.success(listenService.getPassiveListenAccounts(pid));
    }

    /**
     * 获取主动监听账号
     * GET /api/listen/active
     */
    @GetMapping("/active")
    public ApiResponse<List<AccountListenStatusDTO>> getActiveAccounts(
            @RequestParam(required = false) Long pid) {
        return ApiResponse.success(listenService.getActiveListenAccounts(pid));
    }

    /**
     * 更新账号监听模式
     * PUT /api/listen/accounts/{accountId}/pattern
     */
    @PutMapping("/accounts/{accountId}/pattern")
    public ApiResponse<Map<String, Object>> updatePattern(
            @PathVariable Long accountId,
            @RequestParam Integer pattern) {
        listenService.updateListenPattern(accountId, pattern);
        return ApiResponse.success(Map.of(
                "accountId", accountId,
                "pattern", pattern,
                "patternName", pattern == 0 ? "被动监听" : "主动监听"
        ));
    }
}
