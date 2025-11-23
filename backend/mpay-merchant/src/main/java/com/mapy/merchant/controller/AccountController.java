package com.mapy.merchant.controller;

import com.mapy.common.response.ApiResponse;
import com.mapy.common.response.PageResponse;
import com.mapy.merchant.dto.AccountCreateRequest;
import com.mapy.merchant.dto.AccountSummary;
import com.mapy.merchant.dto.ChannelCreateRequest;
import com.mapy.merchant.service.AccountService;
import jakarta.validation.Valid;
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
            @RequestParam Long pid,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) Integer pattern,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(
                accountService.listAccounts(pid, platform, state, pattern, page, pageSize));
    }

    @PostMapping
    public ApiResponse<AccountSummary> addAccount(
            @RequestParam Long pid,
            @RequestBody @Valid AccountCreateRequest request) {
        return ApiResponse.success(accountService.createAccount(pid, request));
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
    public ApiResponse<List<com.mapy.merchant.dto.ChannelSummary>> listChannels(@PathVariable Long id) {
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
}
