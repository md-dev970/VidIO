package com.mddev.videoservice.dto;

import com.mddev.videoservice.entity.VideoEntity;
import com.mddev.videoservice.entity.VideoStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record VideoResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long fileSize,
        String originalPath,
        String thumbnailPath,
        String processedPath,
        String ownerId,
        String ownerUsername,
        String ownerEmail,
        VideoStatus status,
        Double durationSeconds,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VideoResponse from(VideoEntity video) {
        return new VideoResponse(
                video.getId(),
                video.getOriginalFilename(),
                video.getContentType(),
                video.getFileSize(),
                video.getOriginalPath(),
                video.getThumbnailPath(),
                video.getProcessedPath(),
                video.getOwnerId(),
                video.getOwnerUsername(),
                video.getOwnerEmail(),
                video.getStatus(),
                video.getDurationSeconds(),
                video.getErrorMessage(),
                video.getCreatedAt(),
                video.getUpdatedAt()
        );
    }
}
