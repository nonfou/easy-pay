package com.github.nonfou.mpay.dto.account;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 账号交易流水 DTO
 */
@Data
@Builder
public class AccountTransactionDTO {

    /** 订单号 */
    private String orderId;

    /** 商户订单号 */
    private String outTradeNo;

    /** 支付类型 */
    private String type;

    /** 商品名称 */
    private String name;

    /** 订单金额 */
    private Double money;

    /** 实付金额 */
    private Double reallyPrice;

    /** 订单状态 */
    private Integer state;

    /** 状态名称 */
    private String stateName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 通道名称 */
    private String channelName;
}
