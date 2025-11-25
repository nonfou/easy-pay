package com.github.nonfou.mpay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
public class MerchantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pid;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    // P2: 用户角色 (0=普通用户, 1=管理员)
    @Column(nullable = false)
    private Integer role = 0;

    // 用户名
    @Column
    private String username;

    // 登录密码 (BCrypt 加密)
    @Column
    private String password;

    // 邮箱
    @Column
    private String email;

    // 状态 (0=禁用, 1=启用)
    @Column
    private Integer state = 1;

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return role != null && role == 1;
    }
}
