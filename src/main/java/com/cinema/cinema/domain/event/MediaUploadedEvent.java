package com.cinema.cinema.domain.event;

import java.time.Instant;

public record MediaUploadedEvent(
        String filename,
        String contentType,
        long size,
        Instant uploadedAt
) {
    public MediaUploadedEvent {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }

    public MediaUploadedEvent(String filename, String contentType, long size) {
        this(filename, contentType, size, Instant.now());
    }
}
