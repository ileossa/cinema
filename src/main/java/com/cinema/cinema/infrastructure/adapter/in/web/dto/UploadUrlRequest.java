package com.cinema.cinema.infrastructure.adapter.in.web.dto;

public record UploadUrlRequest(
        String filename,
        String contentType
) {
}
