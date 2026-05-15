package com.mddev.videoservice.dto;

import java.time.Instant;

public record PresignedUrlResponse(
        String url,
        Instant expiresAt
) {
}
