package com.threadtrades.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dev-only local-disk implementation of {@link StorageService} (ROADMAP.md
 * #1). Files are written under a UUID name -- never the client-supplied
 * filename -- both to avoid collisions and to close off path traversal via a
 * crafted filename (AUDIT.md #6.4).
 */
@Service
public class LocalDiskStorageService implements StorageService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path rootDirectory;

    public LocalDiskStorageService(@Value("${app.storage.local-path}") String localPath) {
        this.rootDirectory = Paths.get(localPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not initialize storage directory: " + rootDirectory, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidUploadException("Uploaded file is empty");
        }
        String extension = EXTENSION_BY_CONTENT_TYPE.get(file.getContentType());
        if (extension == null) {
            throw new UnsupportedImageTypeException(file.getContentType());
        }

        String filename = UUID.randomUUID() + extension;
        Path target = rootDirectory.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
        return "/uploads/" + filename;
    }
}
