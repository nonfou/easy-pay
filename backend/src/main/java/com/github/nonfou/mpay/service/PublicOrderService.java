package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;
import com.github.nonfou.mpay.dto.PublicCreateOrderResult;

public interface PublicOrderService {

    PublicCreateOrderResult createOrder(PublicCreateOrderDTO request);
}
