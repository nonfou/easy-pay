package com.github.nonfou.mpay.security;

import com.github.nonfou.mpay.entity.MerchantEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义 UserDetails 实现，封装商户/用户信息
 */
@Getter
public class MerchantUserDetails implements UserDetails {

    private final Long id;
    private final Long pid;
    private final String username;
    private final String password;
    private final Integer role;
    private final Integer state;
    private final Collection<? extends GrantedAuthority> authorities;

    public MerchantUserDetails(MerchantEntity merchant) {
        this.id = merchant.getId();
        this.pid = merchant.getPid();
        this.username = merchant.getUsername();
        this.password = merchant.getPassword();
        this.role = merchant.getRole();
        this.state = merchant.getState();

        // 根据 role 设置权限
        if (merchant.isAdmin()) {
            this.authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );
        } else {
            this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return state != null && state == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return state != null && state == 1;
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return role != null && role == 1;
    }
}
