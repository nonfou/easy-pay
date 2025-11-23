package com.mapy.payment.service.impl;

import com.mapy.common.error.BusinessException;
import com.mapy.common.error.ErrorCode;
import com.mapy.payment.dto.PublicCreateOrderDTO;
import com.mapy.payment.dto.PublicCreateOrderResult;
import com.mapy.payment.service.PublicOrderService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapy.common.error.BusinessException;
import com.mapy.common.error.ErrorCode;
import com.mapy.payment.dto.PublicCreateOrderDTO;
import com.mapy.payment.dto.PublicCreateOrderResult;
import com.mapy.payment.entity.OrderEntity;
import com.mapy.payment.event.OrderEventPublisher;
import com.mapy.payment.repository.OrderRepository;
import com.mapy.payment.service.ChannelSelector;
import com.mapy.payment.service.MerchantSecretService;
import com.mapy.payment.service.PriceAllocator;
import com.mapy.payment.signature.SignatureService;
import com.mapy.payment.support.OrderIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicOrderServiceImpl implements PublicOrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final MerchantSecretService merchantSecretService;
    private final ChannelSelector channelSelector;
    private final PriceAllocator priceAllocator;
    private final SignatureService signatureService;
    private final ObjectMapper objectMapper;

    public PublicOrderServiceImpl(OrderRepository orderRepository,
            OrderEventPublisher orderEventPublisher,
            MerchantSecretService merchantSecretService,
            ChannelSelector channelSelector,
            PriceAllocator priceAllocator,
            SignatureService signatureService,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.merchantSecretService = merchantSecretService;
        this.channelSelector = channelSelector;
        this.priceAllocator = priceAllocator;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PublicCreateOrderResult createOrder(PublicCreateOrderDTO request) {
        if (request.getMoney() == null || request.getMoney().doubleValue() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "money must be greater than zero");
        }
        orderRepository.findByOutTradeNoAndPid(request.getOutTradeNo(), request.getPid())
                .ifPresent(order -> {
                    throw new BusinessException(ErrorCode.CONFLICT, "duplicate outTradeNo");
                });
        String secret = merchantSecretService.getSecret(request.getPid())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "merchant secret not found"));
        if (!signatureService.verify(request, secret)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "invalid signature");
        }
        ChannelSelector.ChannelSelection selection = channelSelector.select(request.getPid(), request.getType())
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "no channel available"));
        OrderEntity order = buildOrderEntity(request, selection);
        orderRepository.save(order);
        orderEventPublisher.publish(order);
        return PublicCreateOrderResult.builder()
                .orderId(order.getOrderId())
                .cashierUrl("/cashier/" + order.getOrderId())
                .build();
    }

    private OrderEntity buildOrderEntity(PublicCreateOrderDTO request, ChannelSelector.ChannelSelection selection) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(OrderIdGenerator.generate("H"));
        entity.setPid(request.getPid());
        entity.setType(request.getType());
        entity.setOutTradeNo(request.getOutTradeNo());
        entity.setNotifyUrl(request.getNotifyUrl());
        entity.setReturnUrl(request.getReturnUrl());
        entity.setName(request.getName());
        entity.setMoney(request.getMoney().doubleValue());
        BigDecimal allocated = priceAllocator.allocate(request.getMoney(), selection.aid(), selection.cid(), request.getType());
        entity.setReallyPrice(allocated.doubleValue());
        entity.setClientIp(request.getClientIp());
        entity.setDevice(request.getDevice());
        entity.setParam(serializeAttach(request));
        entity.setState(0);
        entity.setPatt(1);
        entity.setCreateTime(now);
        entity.setCloseTime(now.plusMinutes(3));
        entity.setPayTime(now);
        entity.setAid(selection.aid());
        entity.setCid(selection.cid());
        entity.setPatt(selection.pattern());
        return entity;
    }

    private String serializeAttach(PublicCreateOrderDTO request) {
        if (request.getAttach() == null || request.getAttach().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(request.getAttach());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "attach serialize error");
        }
    }
}
