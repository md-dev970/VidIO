package com.mddev.videoservice.service;

import com.mddev.videoservice.dto.AdminOverviewResponse;
import com.mddev.videoservice.dto.PresignedUrlResponse;
import com.mddev.videoservice.dto.VideoResponse;
import com.mddev.videoservice.entity.VideoAssetType;
import com.mddev.videoservice.entity.VideoEntity;
import com.mddev.videoservice.entity.VideoStatus;
import com.mddev.videoservice.event.VideoProcessingCompletedEvent;
import com.mddev.videoservice.event.VideoProcessingFailedEvent;
import com.mddev.videoservice.event.VideoUploadedEvent;
import com.mddev.videoservice.exception.ResourceNotFoundException;
import com.mddev.videoservice.producer.VideoEventProducer;
import com.mddev.videoservice.repository.VideoRepository;
import com.mddev.videoservice.security.AuthenticatedUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final FileStorageService fileStorageService;
    private final PresignedUrlService presignedUrlService;
    private final VideoEventProducer videoEventProducer;

    public VideoService(VideoRepository videoRepository,
                        FileStorageService fileStorageService,
                        PresignedUrlService presignedUrlService,
                        VideoEventProducer videoEventProducer) {
        this.videoRepository = videoRepository;
        this.fileStorageService = fileStorageService;
        this.presignedUrlService = presignedUrlService;
        this.videoEventProducer = videoEventProducer;
    }

    @Transactional
    public VideoResponse upload(MultipartFile file, AuthenticatedUser owner) {
        UUID videoId = UUID.randomUUID();
        String originalPath = fileStorageService.saveOriginal(videoId, file);
        LocalDateTime now = LocalDateTime.now();

        VideoEntity video = new VideoEntity();
        video.setId(videoId);
        video.setOriginalFilename(file.getOriginalFilename());
        video.setContentType(file.getContentType());
        video.setFileSize(file.getSize());
        video.setOriginalPath(originalPath);
        video.setOwnerId(owner.id());
        video.setOwnerUsername(owner.username());
        video.setOwnerEmail(owner.email());
        video.setStatus(VideoStatus.UPLOADED);
        video.setCreatedAt(now);
        video.setUpdatedAt(now);

        VideoEntity saved = videoRepository.save(video);
        VideoUploadedEvent event = new VideoUploadedEvent(
                UUID.randomUUID(),
                saved.getId(),
                saved.getOriginalFilename(),
                saved.getOriginalPath(),
                saved.getContentType(),
                saved.getFileSize(),
                Instant.now()
        );
        publishAfterCommit(event);

        return VideoResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public VideoResponse findById(UUID id, AuthenticatedUser owner) {
        return VideoResponse.from(getVideo(id, owner.id()));
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> findAll(AuthenticatedUser owner) {
        return videoRepository.findByOwnerIdOrderByCreatedAtDesc(owner.id()).stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse assetUrl(UUID id, String assetType, AuthenticatedUser owner) {
        return presignedUrlService.createGetUrl(getVideo(id, owner.id()), VideoAssetType.fromPathValue(assetType));
    }

    @Transactional(readOnly = true)
    public VideoResponse findByIdForAdmin(UUID id) {
        return VideoResponse.from(getVideo(id));
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> findAllForAdmin() {
        return videoRepository.findAll().stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse overviewForAdmin() {
        return new AdminOverviewResponse(
                videoRepository.count(),
                videoRepository.countByStatus(VideoStatus.UPLOADED),
                videoRepository.countByStatus(VideoStatus.PROCESSING),
                videoRepository.countByStatus(VideoStatus.COMPLETED),
                videoRepository.countByStatus(VideoStatus.FAILED)
        );
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse assetUrlForAdmin(UUID id, String assetType) {
        return presignedUrlService.createGetUrl(getVideo(id), VideoAssetType.fromPathValue(assetType));
    }

    @Transactional
    public void markCompleted(VideoProcessingCompletedEvent event) {
        VideoEntity video = getVideo(event.videoId());
        video.setStatus(VideoStatus.COMPLETED);
        video.setProcessedPath(event.processedPath());
        video.setThumbnailPath(event.thumbnailPath());
        video.setDurationSeconds(event.durationSeconds());
        video.setErrorMessage(null);
        video.setUpdatedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    @Transactional
    public void markFailed(VideoProcessingFailedEvent event) {
        VideoEntity video = getVideo(event.videoId());
        video.setStatus(VideoStatus.FAILED);
        video.setErrorMessage(event.errorMessage());
        video.setUpdatedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    private VideoEntity getVideo(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found: " + id));
    }

    private VideoEntity getVideo(UUID id, String ownerId) {
        return videoRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found: " + id));
    }

    private void publishAfterCommit(VideoUploadedEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                videoEventProducer.publishVideoUploaded(event);
            }
        });
    }
}
