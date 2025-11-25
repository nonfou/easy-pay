package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.dto.MatchRequest;
import com.github.nonfou.mpay.entity.OrderEntity;
import com.github.nonfou.mpay.notify.NotifyClient;
import com.github.nonfou.mpay.repository.OrderRepository;
import com.github.nonfou.mpay.service.OrderMatchService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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
                .filter(o -> o.getReallyPrice().compareTo(request.getPrice()) == 0)
                .min(Comparator.comparing(OrderEntity::getCreateTime))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "未找到匹配的订单"));
        match.setState(1);
        match.setPayTime(LocalDateTime.now());
        match.setPlatformOrder(request.getPlatformOrder());
        orderRepository.save(match);
        log.info("订单匹配成功: orderId={}, price={}, platformOrder={}",
                match.getOrderId(), request.getPrice(), request.getPlatformOrder());
        notifyClient.notifyMerchant(match);
    }
}
