package com.mapy.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mapy")
public class MpayMerchantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpayMerchantApplication.class, args);
    }
}
