package com.mapy.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private Long pid;

    @Column(nullable = false)
    private String type;

    @Column(name = "out_trade_no", nullable = false)
    private String outTradeNo;

    @Column(name = "notify_url", nullable = false)
    private String notifyUrl;

    @Column(name = "return_url")
    private String returnUrl;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double money;

    @Column(name = "really_price", nullable = false)
    private Double reallyPrice;

    @Column(name = "clientip")
    private String clientIp;

    @Column
    private String device;

    @Column(columnDefinition = "TEXT")
    private String param;

    @Column(nullable = false)
    private Integer state;

    @Column
    private Integer patt;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "close_time")
    private LocalDateTime closeTime;

    @Column(name = "pay_time")
    private LocalDateTime payTime;

    @Column(name = "aid")
    private Long aid;

    @Column(name = "cid")
    private Long cid;

    @Column(name = "platform_order")
    private String platformOrder;
}
