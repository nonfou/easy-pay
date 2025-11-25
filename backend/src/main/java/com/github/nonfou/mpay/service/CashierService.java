package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.cashier.CashierOrderDTO;
import com.github.nonfou.mpay.dto.cashier.CashierOrderStateDTO;
import java.util.Optional;

/**
 * 收银台服务接口
 */
public interface CashierService {

    /**
     * 获取收银台订单详情
     */
    Optional<CashierOrderDTO> getOrderDetail(String orderId);

    /**
     * 获取订单状态（用于轮询）
     */
    Optional<CashierOrderStateDTO> getOrderState(String orderId);
}
