package com.mapy.monitor.service;

import com.mapy.monitor.dto.PaymentRecordDTO;

public interface PaymentMatchService {

    void handlePaymentRecord(PaymentRecordDTO record);
}
