package com.threadtrades.storage;

import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Streams objects written by {@link R2StorageService} back out over HTTP at
 * the URL path it hands out ("/media/{key}"), the R2 counterpart of
 * {@link StorageWebConfig}'s local-disk resource handler. Reads go through
 * these same authenticated credentials rather than R2's public
 * "pub-*.r2.dev" URL -- see ROADMAP.md #3 for why (that URL 404s some
 * objects even with public access enabled and correctly configured, an
 * unresolved Cloudflare-side issue).
 */
@RestController
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "r2")
public class R2MediaController {

    // UUID + the fixed extension set UploadValidation.extensionFor hands out --
    // never the client-supplied filename, so this fully bounds the key shape.
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|gif)$");

    private final S3Client s3Client;
    private final String bucket;

    public R2MediaController(S3Client r2S3Client, @Value("${app.storage.r2.bucket}") String bucket) {
        this.s3Client = r2S3Client;
        this.bucket = bucket;
    }

    @GetMapping("/media/{key}")
    public ResponseEntity<InputStreamResource> get(@PathVariable String key) {
        if (!KEY_PATTERN.matcher(key).matches()) {
            return ResponseEntity.notFound().build();
        }
        ResponseInputStream<GetObjectResponse> object;
        try {
            object = s3Client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        }
        GetObjectResponse metadata = object.response();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .contentLength(metadata.contentLength())
                // Keys are random UUIDs, never reused/overwritten -- safe to cache forever.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(new InputStreamResource(object));
    }
}
