package com.sharenote.storage;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FileValidationService {

    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("doc", Set.of("application/msword")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("xls", Set.of("application/vnd.ms-excel")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
    );
    private static final Set<String> SUSPICIOUS_EXTENSIONS = Set.of(
            "ade", "adp", "apk", "app", "bat", "bin", "cmd", "com", "cpl", "dll", "dmg",
            "exe", "gadget", "hta", "jar", "js", "jse", "lnk", "msi", "msp", "pif",
            "ps1", "scr", "sh", "vb", "vbe", "vbs", "ws", "wsf"
    );
    private static final byte[] OLE_MAGIC = bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);

    private final StorageProperties storageProperties;

    public FileValidationService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public ValidatedFile validate(MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("File is required");
        }

        String submittedFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(submittedFileName)) {
            throw new InvalidFileException("File name is invalid");
        }

        String originalFileName = StringUtils.cleanPath(submittedFileName);
        if (file.isEmpty()) {
            throw new InvalidFileException("File is required");
        }
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new InvalidFileException("File exceeds maximum allowed size");
        }
        if (!StringUtils.hasText(originalFileName)
                || originalFileName.contains("..")
                || originalFileName.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidFileException("File name is invalid");
        }

        String extension = getExtension(originalFileName);
        if (SUSPICIOUS_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("File type is not allowed");
        }

        Set<String> allowedContentTypes = ALLOWED_CONTENT_TYPES_BY_EXTENSION.get(extension);
        if (allowedContentTypes == null || !allowedContentTypes.contains(file.getContentType())) {
            throw new InvalidFileException("File type is not allowed");
        }

        byte[] fileBytes = readBytes(file);
        validateFileSignature(extension, fileBytes);

        return new ValidatedFile(
                originalFileName,
                extension,
                file.getContentType(),
                file.getSize(),
                fileBytes
        );
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new InvalidFileException("Could not read uploaded file");
        }
    }

    private void validateFileSignature(String extension, byte[] bytes) {
        if (hasExecutableSignature(bytes)) {
            throw new InvalidFileException("File content is not allowed");
        }

        boolean validSignature = switch (extension) {
            case "pdf" -> startsWith(bytes, "%PDF".getBytes());
            case "jpg", "jpeg" -> startsWith(bytes, bytes(0xFF, 0xD8, 0xFF));
            case "png" -> startsWith(bytes, bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A));
            case "gif" -> startsWith(bytes, "GIF87a".getBytes()) || startsWith(bytes, "GIF89a".getBytes());
            case "webp" -> startsWith(bytes, "RIFF".getBytes()) && bytes.length >= 12
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            case "docx", "xlsx", "pptx" -> startsWith(bytes, "PK".getBytes());
            case "doc", "xls", "ppt" -> startsWith(bytes, OLE_MAGIC);
            default -> false;
        };

        if (!validSignature) {
            throw new InvalidFileException("File content does not match the declared type");
        }
    }

    private boolean hasExecutableSignature(byte[] bytes) {
        return startsWith(bytes, "MZ".getBytes())
                || startsWith(bytes, bytes(0x7F, 0x45, 0x4C, 0x46))
                || startsWith(bytes, "#!".getBytes())
                || startsWithIgnoreCase(bytes, "@echo".getBytes());
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length
                && Arrays.equals(Arrays.copyOf(bytes, prefix.length), prefix);
    }

    private boolean startsWithIgnoreCase(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (Character.toLowerCase((char) bytes[index]) != Character.toLowerCase((char) prefix[index])) {
                return false;
            }
        }
        return true;
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            throw new InvalidFileException("File extension is required");
        }
        return fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            bytes[index] = (byte) values[index];
        }
        return bytes;
    }
}
