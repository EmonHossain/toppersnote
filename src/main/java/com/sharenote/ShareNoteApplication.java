package com.sharenote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShareNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareNoteApplication.class, args);
    }
}
