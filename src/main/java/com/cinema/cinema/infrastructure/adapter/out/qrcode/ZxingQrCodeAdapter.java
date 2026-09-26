package com.cinema.cinema.infrastructure.adapter.out.qrcode;

import com.cinema.cinema.domain.port.out.QrCodeGeneratorOut;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ZxingQrCodeAdapter implements QrCodeGeneratorOut {

    private static final int DEFAULT_SIZE = 250;

    @Override
    public byte[] generateQrCodeImage(String content, int width, int height) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("QR code content cannot be null or empty");
        }
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate QR code image", e);
        }
    }

    @Override
    public String generateQrCodeBase64(String content) {
        byte[] imageBytes = generateQrCodeImage(content, DEFAULT_SIZE, DEFAULT_SIZE);
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
