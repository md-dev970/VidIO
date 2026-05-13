# Kafka Events

VidIO uses Apache Kafka for asynchronous communication between `video-service` and `processing-service`.

## Topics

| Topic | Producer | Consumer |
| --- | --- | --- |
| `video.uploaded` | `video-service` | `processing-service` |
| `video.processing.completed` | `processing-service` | `video-service` |
| `video.processing.failed` | `processing-service` | `video-service` |

Kafka runs in Docker as `apache/kafka:3.7.0` in single-node KRaft mode.

Kafka UI:

```text
http://localhost:8085
```

## VideoUploadedEvent

Published after `video-service` successfully stores the uploaded file and saves the video metadata row.

```json
{
  "eventId": "uuid",
  "videoId": "uuid",
  "originalFilename": "demo.mp4",
  "inputPath": "/storage/original/video-id.mp4",
  "contentType": "video/mp4",
  "fileSize": 12345678,
  "timestamp": "2026-05-12T10:00:00Z"
}
```

## VideoProcessingCompletedEvent

Published after `processing-service` creates a thumbnail, converts the video to 720p, and extracts duration.

```json
{
  "eventId": "uuid",
  "videoId": "uuid",
  "processedPath": "/storage/processed/video-id_720p.mp4",
  "thumbnailPath": "/storage/thumbnails/video-id.jpg",
  "durationSeconds": 58.4,
  "timestamp": "2026-05-12T10:02:00Z"
}
```

When consumed, `video-service` updates the video row to:

```text
COMPLETED
```

## VideoProcessingFailedEvent

Published when FFmpeg processing fails.

```json
{
  "eventId": "uuid",
  "videoId": "uuid",
  "errorMessage": "FFmpeg processing failed",
  "timestamp": "2026-05-12T10:02:00Z"
}
```

When consumed, `video-service` updates the video row to:

```text
FAILED
```
