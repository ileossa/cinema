package com.cinema.cinema.domain;

import com.cinema.cinema.domain.event.MediaUploadedEvent;
import com.cinema.cinema.domain.port.out.EventPublisherOut;
import com.cinema.cinema.domain.port.out.MediaStorageOut;
import com.cinema.cinema.domain.service.MediaService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MediaServiceTest {

    @Test
    void should_confirm_upload_when_file_exists() {
        MediaStorageOut mediaStorageMock = mock(MediaStorageOut.class);
        EventPublisherOut eventPublisherMock = mock(EventPublisherOut.class);

        when(mediaStorageMock.fileExists("poster.jpg")).thenReturn(true);
        when(mediaStorageMock.getFileSize("poster.jpg")).thenReturn(1024L);
        when(mediaStorageMock.getContentType("poster.jpg")).thenReturn("image/jpeg");

        MediaService mediaService = new MediaService(mediaStorageMock, eventPublisherMock);

        MediaUploadedEvent event = mediaService.confirmUpload("poster.jpg");

        assertNotNull(event);
        assertEquals("poster.jpg", event.filename());
        assertEquals("image/jpeg", event.contentType());
        assertEquals(1024L, event.size());
        verify(eventPublisherMock, times(1)).publishEvent(any(MediaUploadedEvent.class));
    }

    @Test
    void should_throw_exception_when_file_does_not_exist() {
        MediaStorageOut mediaStorageMock = mock(MediaStorageOut.class);
        when(mediaStorageMock.fileExists("missing.jpg")).thenReturn(false);

        MediaService mediaService = new MediaService(mediaStorageMock);

        assertThrows(IllegalArgumentException.class, () -> mediaService.confirmUpload("missing.jpg"));
    }

    @Test
    void should_throw_exception_when_filename_is_blank() {
        MediaStorageOut mediaStorageMock = mock(MediaStorageOut.class);
        MediaService mediaService = new MediaService(mediaStorageMock);

        assertThrows(IllegalArgumentException.class, () -> mediaService.confirmUpload("   "));
        assertThrows(IllegalArgumentException.class, () -> mediaService.confirmUpload(null));
    }
}
