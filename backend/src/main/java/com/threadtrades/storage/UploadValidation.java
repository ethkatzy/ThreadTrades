package com.threadtrades.storage;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shared upload validation for every {@link StorageService} implementation --
 * rejects empty files and pins the stored file's extension to a fixed
 * allowlist derived from the declared content type, never the client-supplied
 * filename (AUDIT.md #6.4).
 */
final class UploadValidation {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private UploadValidation() {
    }

    static String extensionFor(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidUploadException("Uploaded file is empty");
        }
        String extension = EXTENSION_BY_CONTENT_TYPE.get(file.getContentType());
        if (extension == null) {
            throw new UnsupportedImageTypeException(file.getContentType());
        }
        return extension;
    }
}
