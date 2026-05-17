package com.sharenote.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "storage.notes", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalNoteFileStorage implements NoteFileStorage {

    private final StorageProperties storageProperties;
    private final FileValidationService fileValidationService;

    public LocalNoteFileStorage(StorageProperties storageProperties, FileValidationService fileValidationService) {
        this.storageProperties = storageProperties;
        this.fileValidationService = fileValidationService;
    }

    @Override
    public StoredFile store(MultipartFile file) {
        ValidatedFile validatedFile = fileValidationService.validate(file);
        String storedFileName = UUID.randomUUID() + "." + validatedFile.extension();
        Path uploadDirectory = Path.of(storageProperties.directory()).toAbsolutePath().normalize();
        Path targetPath = uploadDirectory.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadDirectory)) {
            throw new InvalidFileException("Invalid file path");
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.write(targetPath, validatedFile.bytes());
        } catch (IOException exception) {
            throw new FileStorageException("Could not store uploaded file", exception);
        }

        return new StoredFile(
                validatedFile.originalFileName(),
                storedFileName,
                validatedFile.contentType(),
                validatedFile.fileSize(),
                storedFileName,
                targetPath.toString()
        );
    }

    @Override
    public void deleteIfExists(StoredFile storedFile) {
        try {
            Files.deleteIfExists(Path.of(storedFile.storageLocation()));
        } catch (IOException ignored) {
            // Best-effort cleanup after a metadata persistence failure.
        }
    }
}
