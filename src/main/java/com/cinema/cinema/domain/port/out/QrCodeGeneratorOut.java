package com.cinema.cinema.domain.port.out;

public interface QrCodeGeneratorOut {
    byte[] generateQrCodeImage(String content, int width, int height);

    String generateQrCodeBase64(String content);
}
