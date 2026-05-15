# Architecture

VidIO uses authenticated Spring Boot services with Kafka-based asynchronous processing and MinIO-backed object storage.

```text
Browser / Postman / curl
   |
   | Bearer JWT from Keycloak
   v
api-service :8081
   |
   | forwards requests + Authorization header
   v
video-service :8082
   |
   | stores metadata + owner claims in PostgreSQL
   | uploads original object to MinIO
   | publishes video.uploaded with object key
   v
Apache Kafka
   |
   v
processing-service :8083
   |
   | downloads original from MinIO
   | runs FFmpeg thumbnail + 720p conversion
   | uploads outputs to MinIO
   | publishes completed / failed event
   v
Apache Kafka
   |
   v
video-service
   |
   | updates video status and output object keys
   v
PostgreSQL
```

## Runtime Components

| Component | Role |
| --- | --- |
| `api-service` | Public edge. Validates JWTs, enforces admin routes, and proxies requests to downstream services. |
| `video-service` | Owns video metadata, ownership filtering, upload handling, upload events, and processing result consumers. |
| `processing-service` | Owns processing jobs and FFmpeg execution. It consumes upload events and publishes completion/failure events. |
| `keycloak` | OIDC identity provider for `USER` and `ADMIN` roles. |
| `minio` | S3-compatible storage for originals, thumbnails, and processed videos. |
| `postgres` | Stores `videos` and `processing_jobs`. |
| `kafka` | Apache Kafka 3.7 running in single-node KRaft mode. |
| `admin-dashboard` | Angular portal where users upload and track their videos, while admins also inspect all videos and jobs. |

## Authorization Model

`api-service`, `video-service`, and `processing-service` are OAuth2 resource servers. Role claims are read from Keycloak's `realm_access.roles` claim and mapped to Spring authorities like `ROLE_ADMIN`.

Normal users can:

- upload videos
- list only their own videos
- read only their own video details

Admins can:

- list all videos
- read any video
- list processing jobs
- view aggregate overview counts

## Storage

The old shared local `storage/` volume has been replaced by MinIO. Database fields with names like `originalPath`, `thumbnailPath`, and `processedPath` now store object keys:

```text
original/{videoId}.mp4
thumbnails/{videoId}.jpg
processed/{videoId}_720p.mp4
```

`processing-service` downloads inputs into a temporary working directory, runs FFmpeg locally, uploads outputs to MinIO, then deletes its temp files.
