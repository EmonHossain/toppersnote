package com.sharenote.ai;

public record AttachedNoteFile(
        String fileName,
        String contentType,
        byte[] bytes
) {
}
