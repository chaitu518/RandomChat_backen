package com.srt.randomchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.srt")
public class RandomChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(RandomChatApplication.class, args);
    }

}
