package com.threadtrades.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Object-storage implementation of {@link StorageService} over any
 * S3-compatible endpoint (ROADMAP.md #1) -- built against Cloudflare R2, but
 * nothing here is R2-specific beyond the property names, so any S3-compatible
 * provider works by pointing app.storage.r2.endpoint elsewhere. Active only
 * when app.storage.provider=r2; survives redeploys/scaling, unlike
 * {@link LocalDiskStorageService}.
 *
 * <p>Returns a backend-relative "/media/{key}" URL, not R2's public
 * "pub-*.r2.dev" URL directly -- {@link R2MediaController} streams the bytes
 * back out using the same credentials used to write them here. See
 * ROADMAP.md #3 (Storage/deployment notes) for why: R2's Public Development
 * URL silently 404s some objects even with public access enabled and the
 * bucket/URL configured correctly, a still-unresolved Cloudflare-side issue.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "r2")
public class R2StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucket;

    public R2StorageService(S3Client r2S3Client, @Value("${app.storage.r2.bucket}") String bucket) {
        this.s3Client = r2S3Client;
        this.bucket = bucket;
    }

    @Override
    public String store(MultipartFile file) {
        String extension = UploadValidation.extensionFor(file);
        String key = UUID.randomUUID() + extension;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
        return "/media/" + key;
    }
}
