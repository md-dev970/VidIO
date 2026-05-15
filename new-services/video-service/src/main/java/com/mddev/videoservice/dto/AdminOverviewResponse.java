package com.mddev.videoservice.dto;

public record AdminOverviewResponse(
        long totalVideos,
        long uploadedVideos,
        long processingVideos,
        long completedVideos,
        long failedVideos
) {
}
