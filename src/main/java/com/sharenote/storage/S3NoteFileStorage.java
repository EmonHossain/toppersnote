package com.sharenote.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "storage.notes", name = "type", havingValue = "s3")
public class S3NoteFileStorage implements NoteFileStorage {

    private final StorageProperties storageProperties;
    private final FileValidationService fileValidationService;
    private final S3Client s3Client;

    public S3NoteFileStorage(
            StorageProperties storageProperties,
            FileValidationService fileValidationService,
            S3Client s3Client
    ) {
        this.storageProperties = storageProperties;
        this.fileValidationService = fileValidationService;
        this.s3Client = s3Client;
    }

    @Override
    public StoredFile store(MultipartFile file) {
        ValidatedFile validatedFile = fileValidationService.validate(file);
        String bucket = requireBucket();
        String storedFileName = UUID.randomUUID() + "." + validatedFile.extension();
        String key = buildKey(storedFileName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(validatedFile.contentType())
                    .contentLength(validatedFile.fileSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(validatedFile.bytes()));
        } catch (SdkException exception) {
            throw new FileStorageException("Could not store uploaded file", exception);
        }

        return new StoredFile(
                validatedFile.originalFileName(),
                storedFileName,
                validatedFile.contentType(),
                validatedFile.fileSize(),
                key,
                "s3://" + bucket + "/" + key
        );
    }

    @Override
    public void deleteIfExists(StoredFile storedFile) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(storedFile.storageKey())
                    .build());
        } catch (SdkException ignored) {
            // Best-effort cleanup after a metadata persistence failure.
        }
    }

    private String requireBucket() {
        if (!StringUtils.hasText(storageProperties.s3Bucket())) {
            throw new FileStorageException("S3 bucket must be configured", null);
        }
        return storageProperties.s3Bucket().trim();
    }

    private String buildKey(String storedFileName) {
        String prefix = storageProperties.s3Prefix();
        if (!StringUtils.hasText(prefix)) {
            return storedFileName;
        }
        return prefix.replaceAll("^/+", "").replaceAll("/+$", "") + "/" + storedFileName;
    }
}
