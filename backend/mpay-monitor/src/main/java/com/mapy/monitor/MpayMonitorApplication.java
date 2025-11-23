package com.mapy.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mapy")
public class MpayMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpayMonitorApplication.class, args);
    }
}
