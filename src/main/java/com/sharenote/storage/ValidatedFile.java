package com.sharenote.storage;

public record ValidatedFile(
        String originalFileName,
        String extension,
        String contentType,
        long fileSize,
        byte[] bytes
) {
}
