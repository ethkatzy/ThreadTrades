package com.threadtrades.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Shared {@link S3Client} for R2, one bean reused by both
 * {@link R2StorageService} (writes) and {@link R2MediaController} (reads) --
 * see ROADMAP.md #3 (Storage/deployment notes) for why reads go through the
 * backend with these same credentials instead of R2's public
 * "pub-*.r2.dev" URL.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "r2")
public class R2Config {

    @Bean
    public S3Client r2S3Client(
            @Value("${app.storage.r2.endpoint}") String endpoint,
            @Value("${app.storage.r2.access-key-id}") String accessKeyId,
            @Value("${app.storage.r2.secret-access-key}") String secretAccessKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                // R2 buckets aren't tied to an AWS region; the SDK still requires a
                // region value to sign requests, and R2 accepts "auto" for it.
                .region(Region.of("auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }
}
