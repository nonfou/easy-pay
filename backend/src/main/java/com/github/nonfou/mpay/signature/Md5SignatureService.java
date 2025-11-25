package com.github.nonfou.mpay.signature;

import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Md5SignatureService implements SignatureService {

    private static final Logger log = LoggerFactory.getLogger(Md5SignatureService.class);

    @Override
    public boolean verify(PublicCreateOrderDTO request, String secret) {
        String signString = SignatureUtils.buildSignString(request) + secret;
        String expected = SignatureUtils.md5(signString);
        boolean match = expected.equalsIgnoreCase(request.getSign());
        if (!match) {
            log.warn("signature mismatch for pid {}, expected {}", request.getPid(), expected);
        }
        return match;
    }
}
