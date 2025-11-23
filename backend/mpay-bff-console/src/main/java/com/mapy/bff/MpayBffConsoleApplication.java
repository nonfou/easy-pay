package com.mapy.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mapy")
public class MpayBffConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpayBffConsoleApplication.class, args);
    }
}
