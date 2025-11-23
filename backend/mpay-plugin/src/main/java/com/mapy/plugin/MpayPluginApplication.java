package com.mapy.plugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mapy")
public class MpayPluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpayPluginApplication.class, args);
    }
}
