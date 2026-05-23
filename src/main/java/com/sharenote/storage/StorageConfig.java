package com.sharenote.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, ProfilePictureStorageProperties.class})
public class StorageConfig {

    @Bean
    @ConditionalOnExpression("'${storage.notes.type:local}' == 's3' || '${storage.profile-pictures.type:local}' == 's3'")
    public S3Client s3Client(
            StorageProperties storageProperties,
            ProfilePictureStorageProperties profilePictureStorageProperties
    ) {
        String region = StringUtils.hasText(storageProperties.s3Region())
                ? storageProperties.s3Region()
                : profilePictureStorageProperties.s3Region();
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    @ConditionalOnExpression("'${storage.notes.type:local}' == 's3' || '${storage.profile-pictures.type:local}' == 's3'")
    public S3Presigner s3Presigner(
            StorageProperties storageProperties,
            ProfilePictureStorageProperties profilePictureStorageProperties
    ) {
        String region = StringUtils.hasText(storageProperties.s3Region())
                ? storageProperties.s3Region()
                : profilePictureStorageProperties.s3Region();
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
