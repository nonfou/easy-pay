package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.repository.MerchantRepository;
import com.github.nonfou.mpay.service.MerchantSecretService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MerchantSecretServiceImpl implements MerchantSecretService {

    private final MerchantRepository merchantRepository;

    public MerchantSecretServiceImpl(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public Optional<String> getSecret(Long pid) {
        return merchantRepository.findByPid(pid).map(m -> m.getSecretKey());
    }
}
