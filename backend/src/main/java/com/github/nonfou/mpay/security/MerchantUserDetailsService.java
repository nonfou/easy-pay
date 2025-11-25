package com.github.nonfou.mpay.security;

import com.github.nonfou.mpay.entity.MerchantEntity;
import com.github.nonfou.mpay.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现
 */
@Service
@RequiredArgsConstructor
public class MerchantUserDetailsService implements UserDetailsService {

    private final MerchantRepository merchantRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MerchantEntity merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return new MerchantUserDetails(merchant);
    }

    /**
     * 根据 PID 加载用户
     */
    public UserDetails loadUserByPid(Long pid) throws UsernameNotFoundException {
        MerchantEntity merchant = merchantRepository.findByPid(pid)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: pid=" + pid));
        return new MerchantUserDetails(merchant);
    }
}
