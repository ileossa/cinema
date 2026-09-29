package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.event.MediaUploadedEvent;

public interface ConfirmMediaUploadUseCase {
    MediaUploadedEvent confirmUpload(String filename);
}
