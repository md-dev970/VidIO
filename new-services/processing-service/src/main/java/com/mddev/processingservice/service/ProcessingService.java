package com.mddev.processingservice.service;

import com.mddev.processingservice.dto.ManualProcessingRequest;
import com.mddev.processingservice.dto.ProcessingJobResponse;
import com.mddev.processingservice.entity.ProcessingJobEntity;
import com.mddev.processingservice.entity.ProcessingJobStatus;
import com.mddev.processingservice.event.VideoProcessingCompletedEvent;
import com.mddev.processingservice.event.VideoProcessingFailedEvent;
import com.mddev.processingservice.event.VideoUploadedEvent;
import com.mddev.processingservice.producer.ProcessingEventProducer;
import com.mddev.processingservice.repository.ProcessingJobRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessingService {

    private final ProcessingJobRepository processingJobRepository;
    private final FfmpegService ffmpegService;
    private final ProcessingEventProducer processingEventProducer;
    private final S3StorageService s3StorageService;

    public ProcessingService(ProcessingJobRepository processingJobRepository,
                             FfmpegService ffmpegService,
                             ProcessingEventProducer processingEventProducer,
                             S3StorageService s3StorageService) {
        this.processingJobRepository = processingJobRepository;
        this.ffmpegService = ffmpegService;
        this.processingEventProducer = processingEventProducer;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public ProcessingJobResponse fakeProcess(ManualProcessingRequest request) {
        ProcessingJobEntity job = createJob(request.videoId(), request.inputPath());
        markProcessing(job);
        sleepForFakeProcessing();
        markCompleted(job, request.inputPath(), null);
        return ProcessingJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<ProcessingJobResponse> findAllJobsForAdmin() {
        return processingJobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProcessingJobResponse::from)
                .toList();
    }

    public void processUploadedVideo(VideoUploadedEvent event) {
        ProcessingJobEntity job = createJob(event.videoId(), event.inputPath());
        try {
            markProcessing(job);

            Path workDirectory = Files.createTempDirectory("vidio-" + event.videoId());
            Path inputPath = workDirectory.resolve(event.videoId() + ".input");
            Path outputPath = workDirectory.resolve(event.videoId() + "_720p.mp4");
            Path thumbnailPath = workDirectory.resolve(event.videoId() + ".jpg");
            String outputKey = "processed/" + event.videoId() + "_720p.mp4";
            String thumbnailKey = "thumbnails/" + event.videoId() + ".jpg";

            s3StorageService.download(event.inputPath(), inputPath);
            ffmpegService.generateThumbnail(inputPath, thumbnailPath);
            ffmpegService.convertTo720p(inputPath, outputPath);
            double durationSeconds = ffmpegService.extractDuration(inputPath);
            s3StorageService.upload(outputKey, outputPath, "video/mp4");
            s3StorageService.upload(thumbnailKey, thumbnailPath, "image/jpeg");

            markCompleted(job, outputKey, thumbnailKey);
            processingEventProducer.publishCompleted(new VideoProcessingCompletedEvent(
                    UUID.randomUUID(),
                    event.videoId(),
                    outputKey,
                    thumbnailKey,
                    durationSeconds,
                    Instant.now()
            ));
            deleteQuietly(inputPath);
            deleteQuietly(outputPath);
            deleteQuietly(thumbnailPath);
            deleteQuietly(workDirectory);
        } catch (Exception exception) {
            markFailed(job, exception.getMessage());
            processingEventProducer.publishFailed(new VideoProcessingFailedEvent(
                    UUID.randomUUID(),
                    event.videoId(),
                    exception.getMessage(),
                    Instant.now()
            ));
        }
    }

    private ProcessingJobEntity createJob(UUID videoId, String inputPath) {
        LocalDateTime now = LocalDateTime.now();
        ProcessingJobEntity job = new ProcessingJobEntity();
        job.setId(UUID.randomUUID());
        job.setVideoId(videoId);
        job.setStatus(ProcessingJobStatus.PENDING);
        job.setInputPath(inputPath);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return processingJobRepository.save(job);
    }

    private void markProcessing(ProcessingJobEntity job) {
        job.setStatus(ProcessingJobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        processingJobRepository.save(job);
    }

    private void markCompleted(ProcessingJobEntity job, String outputPath, String thumbnailPath) {
        job.setStatus(ProcessingJobStatus.COMPLETED);
        job.setOutputPath(outputPath);
        job.setThumbnailPath(thumbnailPath);
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        processingJobRepository.save(job);
    }

    private void markFailed(ProcessingJobEntity job, String errorMessage) {
        job.setStatus(ProcessingJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        processingJobRepository.save(job);
    }

    private void sleepForFakeProcessing() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fake processing was interrupted", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
