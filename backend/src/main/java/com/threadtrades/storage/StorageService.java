package com.threadtrades.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Persists an uploaded file and returns a URL clients can GET to fetch it
 * back. Implementations decide where bytes actually live (local disk,
 * S3-compatible object storage, ...) -- see ROADMAP.md #1. Callers must never
 * depend on the returned URL's shape beyond "it's a fetchable URL".
 */
public interface StorageService {

    String store(MultipartFile file);
}
