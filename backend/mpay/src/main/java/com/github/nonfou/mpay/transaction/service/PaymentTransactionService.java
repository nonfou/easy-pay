package com.github.nonfou.mpay.transaction.service;

import com.github.nonfou.mpay.transaction.entity.PaymentTransactionEntity;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.TransactionStatus;
import com.github.nonfou.mpay.transaction.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付交易记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentTransactionRepository transactionRepository;

    /**
     * 根据ID查询
     */
    @Transactional(readOnly = true)
    public Optional<PaymentTransactionEntity> findById(Long id) {
        return transactionRepository.findById(id);
    }

    /**
     * 根据订单号查询
     */
    @Transactional(readOnly = true)
    public Optional<PaymentTransactionEntity> findByOrderId(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }

    /**
     * 根据系统交易号查询
     */
    @Transactional(readOnly = true)
    public Optional<PaymentTransactionEntity> findByTradeNo(String tradeNo) {
        return transactionRepository.findByTradeNo(tradeNo);
    }

    /**
     * 根据平台交易号查询
     */
    @Transactional(readOnly = true)
    public Optional<PaymentTransactionEntity> findByPlatformTradeNo(String platformTradeNo) {
        return transactionRepository.findByPlatformTradeNo(platformTradeNo);
    }

    /**
     * 根据状态查询
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionEntity> findByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    /**
     * 分页查询 - 多条件
     */
    @Transactional(readOnly = true)
    public Page<PaymentTransactionEntity> findByConditions(
            String orderId,
            String tradeNo,
            PaymentPlatform platform,
            TransactionStatus status,
            String merchantId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable) {
        return transactionRepository.findByConditions(
                orderId, tradeNo, platform, status, merchantId, startTime, endTime, pageable);
    }

    /**
     * 查询超时未支付的订单
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionEntity> findExpiredOrders(int expireMinutes) {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(expireMinutes);
        return transactionRepository.findExpiredOrders(TransactionStatus.PENDING, expireTime);
    }

    /**
     * 统计某时间段内成功的交易数量
     */
    @Transactional(readOnly = true)
    public long countSuccessTransactions(LocalDateTime startTime, LocalDateTime endTime) {
        return transactionRepository.countByStatusAndTimeRange(TransactionStatus.SUCCESS, startTime, endTime);
    }

    /**
     * 创建交易记录
     */
    @Transactional
    public PaymentTransactionEntity create(PaymentTransactionEntity transaction) {
        return transactionRepository.save(transaction);
    }

    /**
     * 更新交易记录
     */
    @Transactional
    public PaymentTransactionEntity update(PaymentTransactionEntity transaction) {
        return transactionRepository.save(transaction);
    }

    /**
     * 更新交易状态
     */
    @Transactional
    public boolean updateStatus(String tradeNo, TransactionStatus status) {
        return transactionRepository.findByTradeNo(tradeNo)
                .map(transaction -> {
                    transaction.setStatus(status);
                    transactionRepository.save(transaction);
                    log.info("更新交易状态: tradeNo={}, status={}", tradeNo, status);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 更新平台交易号
     */
    @Transactional
    public boolean updatePlatformTradeNo(String tradeNo, String platformTradeNo) {
        return transactionRepository.findByTradeNo(tradeNo)
                .map(transaction -> {
                    transaction.setPlatformTradeNo(platformTradeNo);
                    transactionRepository.save(transaction);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 关闭交易
     */
    @Transactional
    public boolean closeTransaction(String tradeNo) {
        return updateStatus(tradeNo, TransactionStatus.CLOSED);
    }

    /**
     * 根据商户ID分页查询
     */
    @Transactional(readOnly = true)
    public Page<PaymentTransactionEntity> findByMerchantId(String merchantId, Pageable pageable) {
        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable);
    }
}
