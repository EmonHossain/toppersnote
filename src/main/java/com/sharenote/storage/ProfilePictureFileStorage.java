package com.sharenote.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ProfilePictureFileStorage {

    StoredFile store(MultipartFile file);

    void deleteIfExists(StoredFile storedFile);
}
