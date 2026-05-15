package com.mddev.processingservice.service;

import com.mddev.processingservice.config.S3StorageProperties;
import com.mddev.processingservice.exception.FfmpegException;
import java.nio.file.Path;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@org.springframework.stereotype.Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public S3StorageService(S3Client s3Client, S3StorageProperties properties) {
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
            throw new FfmpegException("Could not verify S3 bucket: " + properties.getBucket(), exception);
        } catch (SdkClientException exception) {
            throw new FfmpegException("Could not verify S3 bucket: " + properties.getBucket(), exception);
        }
    }

    public void download(String key, Path destination) {
        ensureBucketExists();
        s3Client.getObject(
                GetObjectRequest.builder().bucket(properties.getBucket()).key(key).build(),
                ResponseTransformer.toFile(destination));
    }

    public void upload(String key, Path source, String contentType) {
        ensureBucketExists();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(source));
    }
}
