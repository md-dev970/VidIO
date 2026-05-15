# API

Public base URL:

```text
http://localhost:8081
```

All `/api/**` routes require a Keycloak bearer token except `/health`.

## Get Tokens

User token:

```powershell
$token = (curl.exe -s -X POST "http://localhost:8089/realms/vidio/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=vidio-dashboard" -d "username=user1" -d "password=user123" -d "grant_type=password" | ConvertFrom-Json).access_token
```

Admin token:

```powershell
$adminToken = (curl.exe -s -X POST "http://localhost:8089/realms/vidio/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=vidio-dashboard" -d "username=admin" -d "password=admin123" -d "grant_type=password" | ConvertFrom-Json).access_token
```

## User Video APIs

Upload video:

```http
POST /api/videos
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

PowerShell:

```powershell
curl.exe -v -H "Authorization: Bearer $token" http://localhost:8081/api/videos -F "file=@`"C:\Users\md\Downloads\demo.mp4`";type=video/mp4"
```

Get one owned video:

```powershell
curl.exe -H "Authorization: Bearer $token" http://localhost:8081/api/videos/{id}
```

List owned videos:

```powershell
curl.exe -H "Authorization: Bearer $token" http://localhost:8081/api/videos
```

Create a short-lived URL for an owned object:

```powershell
curl.exe -H "Authorization: Bearer $token" http://localhost:8081/api/videos/{id}/assets/original/url
curl.exe -H "Authorization: Bearer $token" http://localhost:8081/api/videos/{id}/assets/thumbnail/url
curl.exe -H "Authorization: Bearer $token" http://localhost:8081/api/videos/{id}/assets/processed/url
```

Response:

```json
{
  "url": "https://...",
  "expiresAt": "2026-05-14T12:00:00Z"
}
```

The URL expires after 10 minutes by default. A missing video, non-owned video, or asset that has not been generated returns `404`.

For local Docker runs, presigned URLs point to `http://localhost:9000` so the browser can reach MinIO. The services still use Docker's internal `http://minio:9000` endpoint for S3 API calls.

Example response:

```json
{
  "id": "4f308137-a59e-45d4-8f7a-83e7ecdc2634",
  "originalFilename": "demo.mp4",
  "contentType": "video/mp4",
  "fileSize": 1570221,
  "originalPath": "original/4f308137-a59e-45d4-8f7a-83e7ecdc2634.mp4",
  "thumbnailPath": "thumbnails/4f308137-a59e-45d4-8f7a-83e7ecdc2634.jpg",
  "processedPath": "processed/4f308137-a59e-45d4-8f7a-83e7ecdc2634_720p.mp4",
  "status": "COMPLETED",
  "durationSeconds": 58.4,
  "ownerId": "keycloak-subject",
  "ownerUsername": "user1",
  "ownerEmail": "user1@example.com",
  "errorMessage": null,
  "createdAt": "2026-05-13T22:00:00",
  "updatedAt": "2026-05-13T22:00:10"
}
```

## Admin APIs

These require the `ADMIN` role:

```powershell
curl.exe -H "Authorization: Bearer $adminToken" http://localhost:8081/api/admin/videos
curl.exe -H "Authorization: Bearer $adminToken" http://localhost:8081/api/admin/videos/{id}
curl.exe -H "Authorization: Bearer $adminToken" http://localhost:8081/api/admin/videos/{id}/assets/original/url
curl.exe -H "Authorization: Bearer $adminToken" http://localhost:8081/api/admin/jobs
curl.exe -H "Authorization: Bearer $adminToken" http://localhost:8081/api/admin/overview
```

`/api/admin/overview` returns aggregate counts for total, uploaded, processing, completed, and failed videos.

## Health

```text
GET http://localhost:8081/health
GET http://localhost:8082/health
GET http://localhost:8083/health
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
