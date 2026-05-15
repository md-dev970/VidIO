package com.mddev.videoservice.entity;

import com.mddev.videoservice.exception.ResourceNotFoundException;

public enum VideoAssetType {
    ORIGINAL,
    THUMBNAIL,
    PROCESSED;

    public String keyFrom(VideoEntity video) {
        return switch (this) {
            case ORIGINAL -> video.getOriginalPath();
            case THUMBNAIL -> video.getThumbnailPath();
            case PROCESSED -> video.getProcessedPath();
        };
    }

    public static VideoAssetType fromPathValue(String value) {
        try {
            return VideoAssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResourceNotFoundException("Video asset not found: " + value);
        }
    }
}
