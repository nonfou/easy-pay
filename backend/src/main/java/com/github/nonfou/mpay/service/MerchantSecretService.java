package com.github.nonfou.mpay.service;

import java.util.Optional;

public interface MerchantSecretService {

    Optional<String> getSecret(Long pid);
}
