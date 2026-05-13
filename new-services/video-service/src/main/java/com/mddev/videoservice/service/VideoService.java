package com.mddev.videoservice.service;

import com.mddev.videoservice.dto.VideoResponse;
import com.mddev.videoservice.entity.VideoEntity;
import com.mddev.videoservice.entity.VideoStatus;
import com.mddev.videoservice.event.VideoProcessingCompletedEvent;
import com.mddev.videoservice.event.VideoProcessingFailedEvent;
import com.mddev.videoservice.event.VideoUploadedEvent;
import com.mddev.videoservice.exception.ResourceNotFoundException;
import com.mddev.videoservice.producer.VideoEventProducer;
import com.mddev.videoservice.repository.VideoRepository;
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
    private final VideoEventProducer videoEventProducer;

    public VideoService(VideoRepository videoRepository,
                        FileStorageService fileStorageService,
                        VideoEventProducer videoEventProducer) {
        this.videoRepository = videoRepository;
        this.fileStorageService = fileStorageService;
        this.videoEventProducer = videoEventProducer;
    }

    @Transactional
    public VideoResponse upload(MultipartFile file) {
        UUID videoId = UUID.randomUUID();
        String originalPath = fileStorageService.saveOriginal(videoId, file);
        LocalDateTime now = LocalDateTime.now();

        VideoEntity video = new VideoEntity();
        video.setId(videoId);
        video.setOriginalFilename(file.getOriginalFilename());
        video.setContentType(file.getContentType());
        video.setFileSize(file.getSize());
        video.setOriginalPath(originalPath);
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
    public VideoResponse findById(UUID id) {
        return VideoResponse.from(getVideo(id));
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> findAll() {
        return videoRepository.findAll().stream()
                .map(VideoResponse::from)
                .toList();
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

    private void publishAfterCommit(VideoUploadedEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                videoEventProducer.publishVideoUploaded(event);
            }
        });
    }
}
