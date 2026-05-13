package com.mddev.processingservice.dto;

import com.mddev.processingservice.entity.ProcessingJobEntity;
import com.mddev.processingservice.entity.ProcessingJobStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProcessingJobResponse(
        UUID id,
        UUID videoId,
        ProcessingJobStatus status,
        String inputPath,
        String outputPath,
        String thumbnailPath,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProcessingJobResponse from(ProcessingJobEntity job) {
        return new ProcessingJobResponse(
                job.getId(),
                job.getVideoId(),
                job.getStatus(),
                job.getInputPath(),
                job.getOutputPath(),
                job.getThumbnailPath(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
