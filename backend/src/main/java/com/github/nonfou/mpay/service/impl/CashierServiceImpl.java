package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.dto.cashier.CashierOrderDTO;
import com.github.nonfou.mpay.dto.cashier.CashierOrderStateDTO;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.entity.PayChannelEntity;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.repository.PayChannelRepository;
import com.github.nonfou.mpay.service.CashierService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CashierServiceImpl implements CashierService {

    private final OrderRepository orderRepository;
    private final PayChannelRepository payChannelRepository;

    public CashierServiceImpl(OrderRepository orderRepository,
            PayChannelRepository payChannelRepository) {
        this.orderRepository = orderRepository;
        this.payChannelRepository = payChannelRepository;
    }

    @Override
    public Optional<CashierOrderDTO> getOrderDetail(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(this::buildCashierOrderDTO);
    }

    @Override
    public Optional<CashierOrderStateDTO> getOrderState(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(this::buildCashierOrderStateDTO);
    }

    private CashierOrderDTO buildCashierOrderDTO(OrderEntity order) {
        String qrcodeUrl = null;
        if (order.getCid() != null) {
            qrcodeUrl = payChannelRepository.findById(order.getCid())
                    .map(PayChannelEntity::getQrcode)
                    .orElse(null);
        }

        return CashierOrderDTO.builder()
                .orderId(order.getOrderId())
                .type(order.getType())
                .name(order.getName())
                .money(order.getMoney())
                .reallyPrice(order.getReallyPrice())
                .qrcodeUrl(qrcodeUrl)
                .state(order.getState())
                .createTime(order.getCreateTime())
                .closeTime(order.getCloseTime())
                .returnUrl(order.getReturnUrl())
                .build();
    }

    private CashierOrderStateDTO buildCashierOrderStateDTO(OrderEntity order) {
        long expireIn = 0;
        if (order.getState() == 0 && order.getCloseTime() != null) {
            expireIn = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), order.getCloseTime()));
        }

        return CashierOrderStateDTO.builder()
                .orderId(order.getOrderId())
                .state(order.getState())
                .expireIn(expireIn)
                .returnUrl(order.getReturnUrl())
                .build();
    }
}
