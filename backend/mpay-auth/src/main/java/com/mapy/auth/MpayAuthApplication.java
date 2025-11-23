package com.mapy.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mapy")
public class MpayAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpayAuthApplication.class, args);
    }
}
