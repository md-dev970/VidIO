package com.mddev.videoservice.service;

import com.mddev.videoservice.config.S3StorageProperties;
import com.mddev.videoservice.exception.StorageException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public FileStorageService(S3Client s3Client, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (NoSuchBucketException exception) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (AwsServiceException exception) {
            if (exception.statusCode() == 404) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
                return;
            }
            throw new StorageException("Could not verify S3 bucket: " + properties.getBucket(), exception);
        } catch (SdkClientException exception) {
            throw new StorageException("Could not verify S3 bucket: " + properties.getBucket(), exception);
        }
    }

    public String saveOriginal(UUID videoId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }

        String key = "original/" + videoId + extensionFor(file.getOriginalFilename());
        try {
            ensureBucketExists();
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (IOException exception) {
            throw new StorageException("Could not read uploaded video", exception);
        } catch (AwsServiceException | SdkClientException exception) {
            throw new StorageException("Failed to store uploaded video in S3", exception);
        }
    }

    private String extensionFor(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleaned.length() - 1) {
            return ".mp4";
        }
        return cleaned.substring(dotIndex);
    }
}
