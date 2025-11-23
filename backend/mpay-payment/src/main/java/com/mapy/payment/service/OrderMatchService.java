package com.mapy.payment.service;

import com.mapy.payment.dto.MatchRequest;

public interface OrderMatchService {

    void matchPayment(MatchRequest request);
}
