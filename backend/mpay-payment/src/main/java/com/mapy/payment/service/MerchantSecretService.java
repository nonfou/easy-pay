package com.mapy.payment.service;

import java.util.Optional;

public interface MerchantSecretService {

    Optional<String> getSecret(Long pid);
}
