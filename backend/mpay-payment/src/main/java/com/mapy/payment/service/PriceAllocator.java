package com.mapy.payment.service;

import java.math.BigDecimal;

public interface PriceAllocator {

    BigDecimal allocate(BigDecimal target, Long aid, Long cid, String type);
}
