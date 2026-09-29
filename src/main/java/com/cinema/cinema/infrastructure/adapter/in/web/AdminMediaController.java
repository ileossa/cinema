package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.event.MediaUploadedEvent;
import com.cinema.cinema.domain.port.in.ConfirmMediaUploadUseCase;
import com.cinema.cinema.domain.port.out.MediaStorageOut;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.ConfirmUploadRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.UploadUrlRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.UploadUrlResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    private final MediaStorageOut mediaStorageOut;
    private final ConfirmMediaUploadUseCase confirmMediaUploadUseCase;

    public AdminMediaController(MediaStorageOut mediaStorageOut, ConfirmMediaUploadUseCase confirmMediaUploadUseCase) {
        this.mediaStorageOut = mediaStorageOut;
        this.confirmMediaUploadUseCase = confirmMediaUploadUseCase;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(@RequestBody(required = false) UploadUrlRequest request) {
        if (request == null || request.filename() == null || request.filename().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename is required");
        }
        String uploadUrl = mediaStorageOut.generatePresignedUploadUrl(request.filename().trim(), request.contentType());
        return ResponseEntity.ok(new UploadUrlResponse(request.filename().trim(), uploadUrl));
    }

    @PostMapping("/confirm")
    public ResponseEntity<MediaUploadedEvent> confirmUpload(@RequestBody(required = false) ConfirmUploadRequest request) {
        if (request == null || request.filename() == null || request.filename().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename is required");
        }
        try {
            MediaUploadedEvent event = confirmMediaUploadUseCase.confirmUpload(request.filename().trim());
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
