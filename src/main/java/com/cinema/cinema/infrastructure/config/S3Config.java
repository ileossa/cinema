package com.cinema.cinema.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(@Value("${rustfs.url}") String endpointUrl,
                                   @Value("${rustfs.access-key}") String accessKey,
                                   @Value("${rustfs.secret-key}") String secretKey,
                                   @Value("${rustfs.region}") String region) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpointUrl))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public S3Client s3Client(@Value("${rustfs.url}") String endpointUrl,
                             @Value("${rustfs.access-key}") String accessKey,
                             @Value("${rustfs.secret-key}") String secretKey,
                             @Value("${rustfs.region}") String region) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpointUrl))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
