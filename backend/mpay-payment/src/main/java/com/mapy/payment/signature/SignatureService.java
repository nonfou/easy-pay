package com.mapy.payment.signature;

import com.mapy.payment.dto.PublicCreateOrderDTO;

public interface SignatureService {

    boolean verify(PublicCreateOrderDTO request, String secret);
}
