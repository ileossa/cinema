package com.cinema.cinema.domain.port.out;

public interface MediaStorageOut {
    String generateUrlAccesMedia(String filename);

    String generatePresignedUploadUrl(String filename, String contentType);

    boolean fileExists(String filename);

    long getFileSize(String filename);

    String getContentType(String filename);

    byte[] downloadMedia(String filename);

    void deleteMedia(String filename);
}
