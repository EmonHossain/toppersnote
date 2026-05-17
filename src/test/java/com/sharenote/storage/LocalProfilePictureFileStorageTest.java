package com.sharenote.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalProfilePictureFileStorageTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void storeAcceptsImageProfilePicture() throws Exception {
        LocalProfilePictureFileStorage storage = localProfilePictureStorage(5_242_880);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00
                }
        );

        StoredFile storedFile = storage.store(file);

        assertThat(storedFile.originalFileName()).isEqualTo("avatar.png");
        assertThat(storedFile.contentType()).isEqualTo("image/png");
        assertThat(storedFile.storageLocation()).startsWith(tempDirectory.toString());
        try (Stream<Path> files = Files.list(tempDirectory)) {
            assertThat(files.toList()).hasSize(1);
        }
    }

    @Test
    void storeRejectsNonImageFile() {
        LocalProfilePictureFileStorage storage = localProfilePictureStorage(5_242_880);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes()
        );

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File type is not allowed");
    }

    @Test
    void storeRejectsImageOverProfilePictureSizeLimit() {
        LocalProfilePictureFileStorage storage = localProfilePictureStorage(4);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
                }
        );

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File exceeds maximum allowed size");
    }

    private LocalProfilePictureFileStorage localProfilePictureStorage(long maxProfilePictureSizeBytes) {
        StorageProperties noteStorageProperties = new StorageProperties(
                tempDirectory.resolve("notes").toString(),
                10_485_760,
                "local",
                "",
                "notes",
                "us-east-1"
        );
        ProfilePictureStorageProperties profilePictureStorageProperties = new ProfilePictureStorageProperties(
                tempDirectory.toString(),
                maxProfilePictureSizeBytes,
                "local",
                "",
                "profile-pictures",
                "us-east-1"
        );
        return new LocalProfilePictureFileStorage(
                profilePictureStorageProperties,
                new FileValidationService(noteStorageProperties)
        );
    }
}
