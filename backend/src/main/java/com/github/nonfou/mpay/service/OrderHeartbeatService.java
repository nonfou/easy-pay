package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.monitor.OrderHeartbeatDTO;
import java.util.List;

public interface OrderHeartbeatService {

    void publishActiveOrders(List<OrderHeartbeatDTO> orders);

    List<OrderHeartbeatDTO> fetchActiveOrders(String pid);
}
