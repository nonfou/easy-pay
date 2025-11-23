package com.mapy.monitor.service;

import com.mapy.monitor.dto.OrderHeartbeatDTO;
import java.util.List;

public interface OrderHeartbeatService {

    void publishActiveOrders(List<OrderHeartbeatDTO> orders);

    List<OrderHeartbeatDTO> fetchActiveOrders(String pid);
}
