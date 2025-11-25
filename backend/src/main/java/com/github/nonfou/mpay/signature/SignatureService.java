package com.github.nonfou.mpay.signature;

import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;

public interface SignatureService {

    boolean verify(PublicCreateOrderDTO request, String secret);
}
