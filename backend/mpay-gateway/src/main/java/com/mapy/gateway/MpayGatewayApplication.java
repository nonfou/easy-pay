package com.mapy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mapy")
public class MpayGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpayGatewayApplication.class, args);
    }
}
