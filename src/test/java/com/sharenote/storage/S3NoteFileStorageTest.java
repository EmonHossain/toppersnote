package com.sharenote.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;

class S3NoteFileStorageTest {

    private final StorageProperties storageProperties = new StorageProperties(
            "unused",
            10_485_760,
            "s3",
            "notes-bucket",
            "/class-notes/",
            "us-east-1"
    );
    private final FileValidationService fileValidationService = new FileValidationService(storageProperties);
    private final CapturingS3Client s3Client = new CapturingS3Client();
    private final S3NoteFileStorage storage = new S3NoteFileStorage(
            storageProperties,
            fileValidationService,
            s3Client
    );

    @Test
    void storeUploadsFileToConfiguredBucketAndPrefix() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calculus.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes()
        );

        StoredFile storedFile = storage.store(file);

        PutObjectRequest request = s3Client.putObjectRequest;
        assertThat(request.bucket()).isEqualTo("notes-bucket");
        assertThat(request.key()).startsWith("class-notes/");
        assertThat(request.key()).endsWith(".pdf");
        assertThat(request.contentType()).isEqualTo("application/pdf");
        assertThat(s3Client.requestBody).isNotNull();
        assertThat(storedFile.storageKey()).isEqualTo(request.key());
        assertThat(storedFile.storageLocation()).isEqualTo("s3://notes-bucket/" + request.key());
    }

    @Test
    void deleteUsesStoredS3Key() {
        StoredFile storedFile = new StoredFile(
                "calculus.pdf",
                "stored.pdf",
                "application/pdf",
                123,
                "class-notes/stored.pdf",
                "s3://notes-bucket/class-notes/stored.pdf"
        );

        storage.deleteIfExists(storedFile);

        assertThat(s3Client.deleteObjectRequest.bucket()).isEqualTo("notes-bucket");
        assertThat(s3Client.deleteObjectRequest.key()).isEqualTo("class-notes/stored.pdf");
    }

    private static class CapturingS3Client implements S3Client {

        private PutObjectRequest putObjectRequest;
        private RequestBody requestBody;
        private DeleteObjectRequest deleteObjectRequest;

        @Override
        public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
            this.putObjectRequest = putObjectRequest;
            this.requestBody = requestBody;
            return PutObjectResponse.builder().build();
        }

        @Override
        public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
            this.deleteObjectRequest = deleteObjectRequest;
            return DeleteObjectResponse.builder().build();
        }

        @Override
        public String serviceName() {
            return "s3";
        }

        @Override
        public void close() {
        }
    }
}
