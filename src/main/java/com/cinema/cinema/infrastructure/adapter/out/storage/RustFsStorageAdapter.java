package com.cinema.cinema.infrastructure.adapter.out.storage;

import com.cinema.cinema.domain.port.out.MediaStorageOut;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

public class RustFsStorageAdapter implements MediaStorageOut {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String bucketName;

    public RustFsStorageAdapter(S3Presigner s3Presigner, S3Client s3Client, String bucketName) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public RustFsStorageAdapter(S3Presigner s3Presigner, String bucketName) {
        this(s3Presigner, null, bucketName);
    }

    @Override
    public String generateUrlAccesMedia(String filename) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(b -> b.bucket(bucketName).key(filename))
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public String generatePresignedUploadUrl(String filename, String contentType) {
        PutObjectPresignRequest.Builder builder = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30))
                .putObjectRequest(b -> {
                    b.bucket(bucketName).key(filename);
                    if (contentType != null && !contentType.isBlank()) {
                        b.contentType(contentType);
                    }
                });
        return s3Presigner.presignPutObject(builder.build()).url().toString();
    }

    @Override
    public boolean fileExists(String filename) {
        if (s3Client == null) {
            return false;
        }
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(filename).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public long getFileSize(String filename) {
        if (s3Client == null) {
            return 0L;
        }
        HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(filename).build());
        return response.contentLength() != null ? response.contentLength() : 0L;
    }

    @Override
    public String getContentType(String filename) {
        if (s3Client == null) {
            return null;
        }
        HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(filename).build());
        return response.contentType();
    }

    @Override
    public byte[] downloadMedia(String filename) {
        if (s3Client == null) {
            return new byte[0];
        }
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(filename).build(), ResponseTransformer.toBytes()).asByteArray();
    }

    @Override
    public void deleteMedia(String filename) {
        if (s3Client != null) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(filename).build());
        }
    }
}
