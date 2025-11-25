package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.MatchRequest;

public interface OrderMatchService {

    void matchPayment(MatchRequest request);
}
