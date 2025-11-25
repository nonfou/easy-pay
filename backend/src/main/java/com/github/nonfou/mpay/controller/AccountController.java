package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.account.AccountCreateRequest;
import com.github.nonfou.mpay.dto.account.AccountSummary;
import com.github.nonfou.mpay.dto.account.AccountTransactionDTO;
import com.github.nonfou.mpay.dto.account.ChannelCreateRequest;
import com.github.nonfou.mpay.dto.account.ChannelSummary;
import com.github.nonfou.mpay.security.SecurityUtils;
import com.github.nonfou.mpay.service.AccountService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AccountSummary>> listAccounts(
            @RequestParam(required = false) Long pid,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) Integer pattern,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 租户隔离：普通用户只能查询自己的账号
        Long accessiblePid = SecurityUtils.resolveAccessiblePid(pid);
        return ApiResponse.success(
                accountService.listAccounts(accessiblePid, platform, state, pattern, page, pageSize));
    }

    @PostMapping
    public ApiResponse<AccountSummary> addAccount(
            @RequestParam(required = false) Long pid,
            @RequestBody @Valid AccountCreateRequest request) {
        // 租户隔离：普通用户只能为自己创建账号
        Long accessiblePid = SecurityUtils.resolveAccessiblePid(pid);
        return ApiResponse.success(accountService.createAccount(accessiblePid, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateAccount(
            @PathVariable Long id,
            @RequestBody @Valid AccountCreateRequest request) {
        accountService.updateAccount(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/state")
    public ApiResponse<Void> changeState(
            @PathVariable Long id,
            @RequestParam Integer state) {
        accountService.updateAccountState(id, state);
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAccounts(@RequestBody List<Long> ids) {
        accountService.deleteAccounts(ids);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/channels")
    public ApiResponse<List<ChannelSummary>> listChannels(@PathVariable Long id) {
        return ApiResponse.success(accountService.listChannels(id));
    }

    @PostMapping("/{id}/channels")
    public ApiResponse<Void> addChannel(
            @PathVariable Long id,
            @RequestBody @Valid ChannelCreateRequest request) {
        accountService.addChannel(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/channels/{cid}")
    public ApiResponse<Void> deleteChannel(@PathVariable Long cid) {
        accountService.deleteChannel(cid);
        return ApiResponse.success();
    }

    /**
     * P1: 查询账号交易流水
     * GET /api/accounts/{id}/transactions
     */
    @GetMapping("/{id}/transactions")
    public ApiResponse<List<AccountTransactionDTO>> getAccountTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDateTime startTime;
        LocalDateTime endTime;

        if (startDate != null && endDate != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                startTime = LocalDate.parse(startDate, formatter).atStartOfDay();
                endTime = LocalDate.parse(endDate, formatter).plusDays(1).atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "日期格式错误，请使用 yyyy-MM-dd 格式");
            }
        } else {
            // 默认查询最近7天
            LocalDate today = LocalDate.now();
            startTime = today.minusDays(7).atStartOfDay();
            endTime = today.plusDays(1).atStartOfDay();
        }

        return ApiResponse.success(accountService.getAccountTransactions(id, startTime, endTime));
    }
}
