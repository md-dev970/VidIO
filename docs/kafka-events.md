# Kafka Events

VidIO uses Apache Kafka for asynchronous communication between `video-service` and `processing-service`.

## Topics

| Topic | Producer | Consumer |
| --- | --- | --- |
| `video.uploaded` | `video-service` | `processing-service` |
| `video.processing.completed` | `processing-service` | `video-service` |
| `video.processing.failed` | `processing-service` | `video-service` |

Kafka runs as `apache/kafka:3.7.0` in single-node KRaft mode.

Kafka UI:

```text
http://localhost:8085
```

## Storage Keys

Events now pass MinIO object keys instead of local filesystem paths. The current Java record field names still use `inputPath`, `processedPath`, and `thumbnailPath`, but their values are object keys such as:

```text
original/{videoId}.mp4
processed/{videoId}_720p.mp4
thumbnails/{videoId}.jpg
```

## VideoUploadedEvent

Published after `video-service` successfully uploads the original object to MinIO and saves the video metadata row.

```json
{
  "eventId": "uuid",
  "videoId": "uuid",
  "originalFilename": "demo.mp4",
  "inputPath": "original/video-id.mp4",
  "contentType": "video/mp4",
  "fileSize": 12345678,
  "timestamp": "2026-05-13T10:00:00Z"
}
```

## VideoProcessingCompletedEvent

Published after `processing-service` downloads the original object, creates a thumbnail, converts the video to 720p, uploads outputs, and extracts duration.

```json
{
  "eventId": "uuid",
  "videoId": "uuid",
  "processedPath": "processed/video-id_720p.mp4",
  "thumbnailPath": "thumbnails/video-id.jpg",
  "durationSeconds": 58.4,
  "timestamp": "2026-05-13T10:02:00Z"
}
```

## VideoProcessingFailedEvent

Published when processing fails.

```json
{
  "eventId": "uuid",
  "videoId": "uuid",
  "errorMessage": "FFmpeg processing failed",
  "timestamp": "2026-05-13T10:02:00Z"
}
```

