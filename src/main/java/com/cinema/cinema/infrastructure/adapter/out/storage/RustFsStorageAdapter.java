package com.cinema.cinema.infrastructure.adapter.out.storage;

import com.cinema.cinema.domain.port.out.MediaStoragePort;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;


public class RustFsStorageAdapter implements MediaStoragePort {

    private final S3Presigner s3Presigner;
    private final String bucketName;

    public RustFsStorageAdapter(S3Presigner s3Presigner, String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }


    @Override
    public String generateUrlAccesMedia(String filename) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(b -> b.bucket(bucketName).key(filename))
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
