package com.mddev.processingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ManualProcessingRequest(
        @NotNull UUID videoId,
        @NotBlank String inputPath
) {
}
