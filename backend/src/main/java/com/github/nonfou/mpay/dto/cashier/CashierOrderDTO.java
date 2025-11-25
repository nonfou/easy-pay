package com.github.nonfou.mpay.dto.cashier;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 收银台订单详情 DTO
 */
@Value
@Builder
public class CashierOrderDTO {
    String orderId;
    String type;
    String name;
    Double money;
    Double reallyPrice;
    String qrcodeUrl;
    Integer state;
    LocalDateTime createTime;
    LocalDateTime closeTime;
    String returnUrl;
}
