package com.sharenote.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "storage.notes", name = "type", havingValue = "s3")
    public S3Client s3Client(StorageProperties storageProperties) {
        return S3Client.builder()
                .region(Region.of(storageProperties.s3Region()))
                .build();
    }
}
