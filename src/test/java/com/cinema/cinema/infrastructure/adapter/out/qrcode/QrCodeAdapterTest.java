package com.cinema.cinema.infrastructure.adapter.out.qrcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeAdapterTest {

    private final ZxingQrCodeAdapter qrCodeAdapter = new ZxingQrCodeAdapter();

    @Test
    void generateQrCodeImage_shouldReturnNonEmptyByteArray() {
        byte[] imageBytes = qrCodeAdapter.generateQrCodeImage("https://cinema.example.com/tickets/123", 200, 200);

        assertNotNull(imageBytes);
        assertTrue(imageBytes.length > 0);
    }

    @Test
    void generateQrCodeBase64_shouldReturnValidBase64String() {
        String base64 = qrCodeAdapter.generateQrCodeBase64("https://cinema.example.com/tickets/123");

        assertNotNull(base64);
        assertFalse(base64.isBlank());
    }

    @Test
    void generateQrCodeImage_withNullOrBlank_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> qrCodeAdapter.generateQrCodeImage(null, 200, 200));
        assertThrows(IllegalArgumentException.class, () -> qrCodeAdapter.generateQrCodeImage("   ", 200, 200));
    }
}
