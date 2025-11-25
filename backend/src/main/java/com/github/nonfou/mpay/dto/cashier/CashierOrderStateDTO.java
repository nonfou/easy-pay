package com.github.nonfou.mpay.dto.cashier;

import lombok.Builder;
import lombok.Value;

/**
 * 收银台订单状态 DTO
 */
@Value
@Builder
public class CashierOrderStateDTO {
    String orderId;
    Integer state;
    Long expireIn;
    String returnUrl;
}
