package com.sharenote.storage;

public record StoredFile(
        String originalFileName,
        String storedFileName,
        String contentType,
        long fileSize,
        String storageKey,
        String storageLocation
) {
}
