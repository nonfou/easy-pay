package com.github.nonfou.mpay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pay_channel")
@Getter
@Setter
public class PayChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private PayAccountEntity account;

    @Column(nullable = false)
    private String channel;

    @Column
    private String qrcode;

    @Column(name = "last_time")
    private LocalDateTime lastTime;

    @Column(nullable = false)
    private Integer state;

    @Column
    private String type;
}
