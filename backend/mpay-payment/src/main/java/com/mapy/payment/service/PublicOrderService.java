package com.mapy.payment.service;

import com.mapy.payment.dto.PublicCreateOrderDTO;
import com.mapy.payment.dto.PublicCreateOrderResult;

public interface PublicOrderService {

    PublicCreateOrderResult createOrder(PublicCreateOrderDTO request);
}
