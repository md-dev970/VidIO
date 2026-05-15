package com.mddev.videoservice.service;

import com.mddev.videoservice.config.S3StorageProperties;
import com.mddev.videoservice.dto.PresignedUrlResponse;
import com.mddev.videoservice.entity.VideoAssetType;
import com.mddev.videoservice.entity.VideoEntity;
import com.mddev.videoservice.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class PresignedUrlService {

    private final S3Presigner presigner;
    private final S3StorageProperties properties;

    public PresignedUrlService(S3Presigner presigner, S3StorageProperties properties) {
        this.presigner = presigner;
        this.properties = properties;
    }

    public PresignedUrlResponse createGetUrl(VideoEntity video, VideoAssetType assetType) {
        String key = assetType.keyFrom(video);
        if (!StringUtils.hasText(key)) {
            throw new ResourceNotFoundException("Video asset not found: " + assetType);
        }

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(properties.getPresignedUrlExpirationMinutes()));
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(properties.getPresignedUrlExpirationMinutes()))
                .getObjectRequest(objectRequest)
                .build();

        return new PresignedUrlResponse(
                presigner.presignGetObject(presignRequest).url().toString(),
                expiresAt
        );
    }
}
