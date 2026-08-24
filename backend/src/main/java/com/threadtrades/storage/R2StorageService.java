package com.threadtrades.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Object-storage implementation of {@link StorageService} over any
 * S3-compatible endpoint (ROADMAP.md #1) -- built against Cloudflare R2, but
 * nothing here is R2-specific beyond the property names, so any S3-compatible
 * provider works by pointing app.storage.r2.endpoint elsewhere. Active only
 * when app.storage.provider=r2; survives redeploys/scaling, unlike
 * {@link LocalDiskStorageService}.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "r2")
public class R2StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public R2StorageService(
            @Value("${app.storage.r2.endpoint}") String endpoint,
            @Value("${app.storage.r2.bucket}") String bucket,
            @Value("${app.storage.r2.public-base-url}") String publicBaseUrl,
            @Value("${app.storage.r2.access-key-id}") String accessKeyId,
            @Value("${app.storage.r2.secret-access-key}") String secretAccessKey) {
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                // R2 buckets aren't tied to an AWS region; the SDK still requires a
                // region value to sign requests, and R2 accepts "auto" for it.
                .region(Region.of("auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
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
        return publicBaseUrl + key;
    }
}
