# VidIO Video Processing Platform

VidIO is a Spring Boot microservices MVP for asynchronous video processing. A client uploads a video through `api-service`, `video-service` stores the original file and metadata, Kafka carries processing events, and `processing-service` uses FFmpeg to generate a thumbnail plus a 720p MP4.

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Web MVC
- Spring Data JPA
- PostgreSQL 16
- Apache Kafka 3.7 in KRaft mode
- Kafka UI
- Docker Compose
- FFmpeg

## Services

| Service | Port | Responsibility |
| --- | --- | --- |
| `api-service` | `8081` | Public API proxy |
| `video-service` | `8082` | Uploads, metadata, storage paths, video status, Kafka result consumers |
| `processing-service` | `8083` | Kafka worker, job tracking, FFmpeg thumbnail and 720p conversion |
| `kafka-ui` | `8085` | Kafka topic inspection |
| `postgres` | `5432` | MVP database |

## Project Layout

```text
new-services/
  api-service/
  video-service/
  processing-service/
docs/
storage/
  original/
  processed/
  thumbnails/
docker-compose.yml
```

## Run Locally

Start the full stack:

```powershell
docker compose up --build
```

The Dockerfiles build the service jars inside Docker with Java 17 and Maven, skipping tests during image creation. The processing image installs FFmpeg.

Kafka UI is available at:

```text
http://localhost:8085
```

## Health Checks

```powershell
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
```

Expected responses:

```text
API Service Running
Video Service Running
Processing Service Running
```

## Test The Flow

Upload a video through the public API:

```powershell
curl.exe -v http://localhost:8081/api/videos -F "file=@`"C:\Users\md\Downloads\demo.mp4`";type=video/mp4"
```

The upload limit is configured as `100MB` in both `api-service` and `video-service`.

Copy the returned `id`, then check status:

```powershell
curl http://localhost:8081/api/videos/{id}
```

The status should move through `UPLOADED` / `PROCESSING` and finish as `COMPLETED`. Generated files should appear under:

```text
storage/original/
storage/processed/
storage/thumbnails/
```

## Kafka Topics

- `video.uploaded`
- `video.processing.completed`
- `video.processing.failed`

## Database

The MVP uses one PostgreSQL database:

```text
video_platform_db
```

Main tables:

- `videos`
- `processing_jobs`

Hibernate `ddl-auto: update` is used for local development.

## Verification Status

- Context tests pass for all three services.
- Docker Compose starts PostgreSQL, Apache Kafka, Kafka UI, and all services.
- End-to-end upload and processing flow has been manually verified.

## Future Improvements

- JWT authentication
- User accounts
- S3 storage
- Retry and dead-letter topics
- Frontend dashboard
- Monitoring and tracing
