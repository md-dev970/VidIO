package com.mddev.processingservice.event;

import java.time.Instant;
import java.util.UUID;

public record VideoUploadedEvent(
        UUID eventId,
        UUID videoId,
        String originalFilename,
        String inputPath,
        String contentType,
        long fileSize,
        Instant timestamp
) {
}
