# Architecture

VidIO uses three Spring Boot services with Kafka-based asynchronous processing.

```text
Client / Postman / curl
   |
   v
api-service :8081
   |
   | forwards multipart upload/status requests
   v
video-service :8082
   |
   | stores original file + metadata
   | publishes video.uploaded
   v
Apache Kafka
   |
   v
processing-service :8083
   |
   | creates processing job
   | runs FFmpeg thumbnail + 720p conversion
   | publishes video.processing.completed / video.processing.failed
   v
Apache Kafka
   |
   v
video-service
   |
   | updates video status and output paths
   v
PostgreSQL
```

## Runtime Components

| Component | Role |
| --- | --- |
| `api-service` | Thin public edge. It has no business logic and proxies video operations to `video-service`. |
| `video-service` | Owns video metadata, local upload storage, status reads, upload events, and processing result consumers. |
| `processing-service` | Owns processing jobs and FFmpeg execution. It consumes upload events and publishes completion/failure events. |
| `postgres` | Stores `videos` and `processing_jobs` for the MVP. |
| `kafka` | Apache Kafka 3.7 running in single-node KRaft mode. |
| `kafka-ui` | Browser UI for inspecting topics and messages. |

## Ports

| Service | Port |
| --- | --- |
| `api-service` | `8081` |
| `video-service` | `8082` |
| `processing-service` | `8083` |
| `kafka-ui` | `8085` |
| `postgres` | `5432` |
| Kafka external listener | `9092` |

## Storage

The root `storage/` directory is bind-mounted into both `video-service` and `processing-service` as `/storage`.

```text
storage/
  original/
  processed/
  thumbnails/
```

This lets `video-service` write uploaded files and `processing-service` read/process them.
