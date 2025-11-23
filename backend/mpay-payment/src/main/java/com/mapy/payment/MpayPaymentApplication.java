package com.mapy.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mapy")
public class MpayPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpayPaymentApplication.class, args);
    }
}
