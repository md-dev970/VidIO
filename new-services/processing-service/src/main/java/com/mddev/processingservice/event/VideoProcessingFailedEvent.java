package com.mddev.processingservice.event;

import java.time.Instant;
import java.util.UUID;

public record VideoProcessingFailedEvent(
        UUID eventId,
        UUID videoId,
        String errorMessage,
        Instant timestamp
) {
}
