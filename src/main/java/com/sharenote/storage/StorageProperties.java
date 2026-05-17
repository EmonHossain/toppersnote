package com.sharenote.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.notes")
public record StorageProperties(
        String directory,
        long maxFileSizeBytes,
        String type,
        String s3Bucket,
        String s3Prefix,
        String s3Region
) {
}
