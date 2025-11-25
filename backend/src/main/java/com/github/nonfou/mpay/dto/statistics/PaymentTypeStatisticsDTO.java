package com.github.nonfou.mpay.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * 支付方式统计 DTO
 */
@Data
@Builder
public class PaymentTypeStatisticsDTO {

    /** 支付类型 (alipay/wxpay/unionpay) */
    private String type;

    /** 类型名称 */
    private String typeName;

    /** 订单数 */
    private Long orderCount;

    /** 成交金额 */
    private Double totalAmount;

    /** 占比 */
    private Double percentage;
}
