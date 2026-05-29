package com.sharenote.storage;

import com.sharenote.note.Note;
import org.springframework.web.multipart.MultipartFile;

public interface NoteFileStorage {

    StoredFile store(MultipartFile file);

    void deleteIfExists(StoredFile storedFile);

    byte[] read(Note note);

    default String generateDownloadUrl(String storageKey) {
        return null;
    }
}
