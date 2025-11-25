package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.dto.monitor.AccountListenStatusDTO;
import com.github.nonfou.mpay.dto.monitor.ListenPattern;
import com.github.nonfou.mpay.entity.PayAccountEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.repository.PayAccountRepository;
import com.github.nonfou.mpay.service.ListenService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListenServiceImpl implements ListenService {

    private static final String HEARTBEAT_KEY_PREFIX = "mpay:heartbeat:";
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_EXPIRE_MINUTES = 3;

    private final PayAccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;

    public ListenServiceImpl(PayAccountRepository accountRepository,
            OrderRepository orderRepository,
            StringRedisTemplate redisTemplate) {
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<AccountListenStatusDTO> getAccountListenStatus(Long pid) {
        List<PayAccountEntity> accounts = accountRepository.findActiveAccounts(pid);
        return accounts.stream()
                .map(this::toStatusDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountListenStatusDTO> getPassiveListenAccounts(Long pid) {
        List<PayAccountEntity> accounts = accountRepository.findActiveByPattern(pid, ListenPattern.PASSIVE.getCode());
        return accounts.stream()
                .map(this::toStatusDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountListenStatusDTO> getActiveListenAccounts(Long pid) {
        List<PayAccountEntity> accounts = accountRepository.findActiveByPattern(pid, ListenPattern.ACTIVE.getCode());
        return accounts.stream()
                .map(this::toStatusDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateListenPattern(Long accountId, Integer pattern) {
        if (pattern < 0 || pattern > 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "无效的监听模式: " + pattern);
        }

        PayAccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账号不存在: " + accountId));

        account.setPattern(pattern);
        accountRepository.save(account);
    }

    private AccountListenStatusDTO toStatusDTO(PayAccountEntity entity) {
        ListenPattern pattern = ListenPattern.fromCode(entity.getPattern());

        // 检查心跳状态
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + entity.getId();
        String lastHeartbeat = redisTemplate.opsForValue().get(heartbeatKey);
        boolean online = lastHeartbeat != null;

        // 获取活跃订单数
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(DEFAULT_EXPIRE_MINUTES);
        List<?> activeOrders = orderRepository.findActiveOrders(entity.getPid(), expireTime);
        int activeOrderCount = activeOrders.size();

        return AccountListenStatusDTO.builder()
                .accountId(entity.getId())
                .account(entity.getAccount())
                .platform(entity.getPlatform())
                .pattern(entity.getPattern())
                .patternName(pattern.getName())
                .patternDescription(pattern.getDescription())
                .state(entity.getState())
                .online(online)
                .lastHeartbeat(lastHeartbeat != null ? lastHeartbeat :
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .activeOrderCount(activeOrderCount)
                .build();
    }
}
