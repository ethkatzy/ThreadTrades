package com.threadtrades.storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves files written by {@link LocalDiskStorageService} back out over HTTP
 * at the URL path {@link LocalDiskStorageService} hands out ("/uploads/...").
 * The old app never wired this up at all -- uploaded files were write-only
 * (AUDIT.md #5/#10). Only relevant when local disk is the active storage
 * provider -- {@link R2StorageService} hands out its own public URLs
 * directly, nothing for the backend to serve.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class StorageWebConfig implements WebMvcConfigurer {

    private final String localPath;

    public StorageWebConfig(@Value("${app.storage.local-path}") String localPath) {
        this.localPath = localPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(localPath).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
