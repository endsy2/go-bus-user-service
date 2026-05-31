package com.busapp.userservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Primary client — talks to MinIO over the internal endpoint for all
     * server-side operations (bucket creation, upload, delete).
     */
    @Bean
    @Primary
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // Create bucket if it doesn't exist
            boolean bucketExists = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("MinIO bucket '{}' created successfully", bucketName);
            } else {
                log.info("MinIO bucket '{}' already exists", bucketName);
            }

            return client;
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }

    /**
     * Presign-only client — built with the PUBLIC endpoint so generated
     * presigned URLs are signed for (and reachable at) the host the browser
     * actually loads images from. Does no bucket I/O itself.
     */
    @Bean(name = "minioPresignClient")
    public MinioClient minioPresignClient() {
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
