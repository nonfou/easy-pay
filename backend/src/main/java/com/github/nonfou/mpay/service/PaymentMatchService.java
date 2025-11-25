package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.dto.monitor.PaymentRecordDTO;

public interface PaymentMatchService {

    void handlePaymentRecord(PaymentRecordDTO record);
}
