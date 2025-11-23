package com.mapy.payment.service.impl;

import com.mapy.payment.repository.MerchantRepository;
import com.mapy.payment.service.MerchantSecretService;
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
