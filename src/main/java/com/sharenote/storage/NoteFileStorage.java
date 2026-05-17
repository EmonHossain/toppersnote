package com.sharenote.storage;

import org.springframework.web.multipart.MultipartFile;

public interface NoteFileStorage {

    StoredFile store(MultipartFile file);

    void deleteIfExists(StoredFile storedFile);
}
