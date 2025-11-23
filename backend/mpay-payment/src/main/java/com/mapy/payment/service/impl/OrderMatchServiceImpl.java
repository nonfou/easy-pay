package com.mapy.payment.service.impl;

import com.mapy.common.error.BusinessException;
import com.mapy.common.error.ErrorCode;
import com.mapy.payment.dto.MatchRequest;
import com.mapy.payment.entity.OrderEntity;
import com.mapy.payment.notify.NotifyClient;
import com.mapy.payment.repository.OrderRepository;
import com.mapy.payment.service.OrderMatchService;
import java.time.LocalDateTime;
import java.util.Comparator;
atLngimport java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderMatchServiceImpl implements OrderMatchService {

    private static final Logger log = LoggerFactory.getLogger(OrderMatchServiceImpl.class);
    private final OrderRepository orderRepository;
    private final NotifyClient notifyClient;

    public OrderMatchServiceImpl(OrderRepository orderRepository,
            NotifyClient notifyClient) {
        this.orderRepository = orderRepository;
        this.notifyClient = notifyClient;
    }

    @Override
    @Transactional
    public void matchPayment(MatchRequest request) {
        List<OrderEntity> candidates = orderRepository.findByPidAndAidAndTypeAndState(
                request.getPid(), request.getAid(), request.getPayway(), 0);
        OrderEntity match = candidates.stream()
                .filter(o -> Double.compare(o.getReallyPrice(), request.getPrice().doubleValue()) == 0)
                .min(Comparator.comparing(OrderEntity::getCreateTime))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "no matching order"));
        match.setState(1);
        match.setPayTime(LocalDateTime.now());
        match.setPlatformOrder(request.getPlatformOrder());
        orderRepository.save(match);
        log.info("order {} matched by record {}", match.getOrderId(), request);
        notifyClient.notifyMerchant(match);
    }
}
