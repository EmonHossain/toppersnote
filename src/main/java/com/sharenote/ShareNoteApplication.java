package com.sharenote;

import com.sharenote.logging.LoggingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LoggingProperties.class)
public class ShareNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareNoteApplication.class, args);
    }
}
