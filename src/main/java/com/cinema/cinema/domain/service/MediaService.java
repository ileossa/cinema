package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.event.MediaUploadedEvent;
import com.cinema.cinema.domain.port.in.ConfirmMediaUploadUseCase;
import com.cinema.cinema.domain.port.out.EventPublisherOut;
import com.cinema.cinema.domain.port.out.MediaStorageOut;

import java.time.Instant;

public class MediaService implements ConfirmMediaUploadUseCase {

    private final MediaStorageOut mediaStorageOut;
    private final EventPublisherOut eventPublisherOut;

    public MediaService(MediaStorageOut mediaStorageOut, EventPublisherOut eventPublisherOut) {
        this.mediaStorageOut = mediaStorageOut;
        this.eventPublisherOut = eventPublisherOut;
    }

    public MediaService(MediaStorageOut mediaStorageOut) {
        this(mediaStorageOut, null);
    }

    @Override
    public MediaUploadedEvent confirmUpload(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        String trimmedFilename = filename.trim();
        if (!mediaStorageOut.fileExists(trimmedFilename)) {
            throw new IllegalArgumentException("Media file not found in storage: " + trimmedFilename);
        }

        long size = mediaStorageOut.getFileSize(trimmedFilename);
        String contentType = mediaStorageOut.getContentType(trimmedFilename);
        MediaUploadedEvent event = new MediaUploadedEvent(trimmedFilename, contentType, size, Instant.now());

        if (eventPublisherOut != null) {
            eventPublisherOut.publishEvent(event);
        }

        return event;
    }
}
