package com.mddev.processingservice.event;

import java.time.Instant;
import java.util.UUID;

public record VideoProcessingCompletedEvent(
        UUID eventId,
        UUID videoId,
        String processedPath,
        String thumbnailPath,
        Double durationSeconds,
        Instant timestamp
) {
}
