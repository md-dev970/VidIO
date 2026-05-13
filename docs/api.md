# API

Public base URL:

```text
http://localhost:8081
```

The public API is exposed by `api-service` and proxied to `video-service`.

## Upload Video

```http
POST /api/videos
Content-Type: multipart/form-data
```

Multipart field:

```text
file
```

PowerShell example:

```powershell
curl.exe -v http://localhost:8081/api/videos -F "file=@`"C:\Users\md\Downloads\demo.mp4`";type=video/mp4"
```

Upload limit:

```text
100MB
```

Example response:

```json
{
  "id": "4f308137-a59e-45d4-8f7a-83e7ecdc2634",
  "originalFilename": "demo.mp4",
  "contentType": "video/mp4",
  "fileSize": 1570221,
  "status": "UPLOADED",
  "createdAt": "2026-05-12T22:00:00"
}
```

## Get Video

```http
GET /api/videos/{id}
```

Example:

```powershell
curl http://localhost:8081/api/videos/{id}
```

Completed response shape:

```json
{
  "id": "4f308137-a59e-45d4-8f7a-83e7ecdc2634",
  "originalFilename": "demo.mp4",
  "contentType": "video/mp4",
  "fileSize": 1570221,
  "originalPath": "/storage/original/4f308137-a59e-45d4-8f7a-83e7ecdc2634.mp4",
  "thumbnailPath": "/storage/thumbnails/4f308137-a59e-45d4-8f7a-83e7ecdc2634.jpg",
  "processedPath": "/storage/processed/4f308137-a59e-45d4-8f7a-83e7ecdc2634_720p.mp4",
  "status": "COMPLETED",
  "durationSeconds": 58.4,
  "errorMessage": null,
  "createdAt": "2026-05-12T22:00:00",
  "updatedAt": "2026-05-12T22:00:10"
}
```

## List Videos

```http
GET /api/videos
```

Example:

```powershell
curl http://localhost:8081/api/videos
```

## Direct Video Service Endpoints

For local debugging, `video-service` exposes the same core operations:

```text
POST http://localhost:8082/videos
GET  http://localhost:8082/videos/{id}
GET  http://localhost:8082/videos
```

## Health

```text
GET http://localhost:8081/health
GET http://localhost:8082/health
GET http://localhost:8083/health
```

Expected responses:

```text
API Service Running
Video Service Running
Processing Service Running
```

## Status Values

Video statuses:

```text
UPLOADED
PROCESSING
COMPLETED
FAILED
```

Processing job statuses:

```text
PENDING
PROCESSING
COMPLETED
FAILED
```
