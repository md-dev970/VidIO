# VidIO Video Processing Platform

VidIO is a Spring Boot microservices video processing platform. Users authenticate with Keycloak, upload videos through `api-service`, `video-service` stores metadata and S3-compatible object keys, Kafka carries processing events, and `processing-service` uses FFmpeg to generate thumbnails plus 720p MP4 outputs.

## Tech Stack

- Java 17, Spring Boot 3.5
- Spring Security OAuth2 resource server with Keycloak/OIDC
- PostgreSQL 16
- Apache Kafka 3.7 in KRaft mode
- MinIO locally, AWS S3 in the EKS deployment path
- FFmpeg
- Angular user/admin portal
- Docker Compose and Docker Desktop Kubernetes manifests
- Terraform scaffold for AWS EKS, S3, SES, IRSA, and ALB ingress

## Services

| Service | Port | Responsibility |
| --- | --- | --- |
| `api-service` | `8081` | Public authenticated API and admin proxy |
| `video-service` | `8082` | Video metadata, ownership, upload events, processing result consumers |
| `processing-service` | `8083` | Kafka worker, job tracking, FFmpeg, MinIO output uploads |
| `kafka-ui` | `8085` | Kafka topic inspection |
| `admin-dashboard` | `8088` | Angular portal for user uploads and admin visibility |
| `keycloak` | `8089` | OIDC identity provider |
| `minio` | `9000`, `9001` | S3 API and console |
| `postgres` | `5432` | Application database |
| `mailpit` | `8025`, `1025` | Local signup verification email inbox |

## Run Locally

```powershell
docker compose up --build
```

Useful URLs:

```text
API:             http://localhost:8081
VidIO portal:     http://localhost:8088
Kafka UI:        http://localhost:8085
Keycloak:        http://localhost:8089
MinIO console:   http://localhost:9001
```

Demo accounts:

| Username | Password | Roles |
| --- | --- | --- |
| `admin` | `admin123` | `USER`, `ADMIN` |
| `user1` | `user123` | `USER` |
| `user2` | `user123` | `USER` |

MinIO credentials are `minioadmin` / `minioadmin`.

MinIO is the local S3-compatible object store. In production this role is handled by AWS S3. Locally, services talk to MinIO inside Docker at `http://minio:9000`, while browser-opened presigned URLs use `http://localhost:9000`.

Self-registration is enabled in Keycloak. Local verification emails are captured in Mailpit at `http://localhost:8025`. New users receive the `USER` role automatically.

## API Smoke Test

Get a token:

```powershell
$token = (curl.exe -s -X POST "http://localhost:8089/realms/vidio/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=vidio-dashboard" -d "username=user1" -d "password=user123" -d "grant_type=password" | ConvertFrom-Json).access_token
```

Upload a video:

```powershell
curl.exe -v -H "Authorization: Bearer $token" http://localhost:8081/api/videos -F "file=@`"C:\Users\md\Downloads\demo.mp4`";type=video/mp4"
```

Check status:

```powershell
curl.exe -H "Authorization: Bearer $token" http://localhost:8081/api/videos/{id}
```

Open an owned asset with a fresh presigned URL:

```powershell
$asset = curl.exe -s -H "Authorization: Bearer $token" http://localhost:8081/api/videos/{id}/assets/original/url | ConvertFrom-Json
Start-Process $asset.url
```

Admin overview:

```powershell
$adminToken = (curl.exe -s -X POST "http://localhost:8089/realms/vidio/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=vidio-dashboard" -d "username=admin" -d "password=admin123" -d "grant_type=password" | ConvertFrom-Json).access_token
curl.exe -H "Authorization: Bearer $adminToken" http://localhost:8081/api/admin/overview
```

Uploads are limited to `100MB`.

## Kubernetes

Docker Compose remains the fastest local path. Kubernetes manifests live in `k8s/` for Docker Desktop Kubernetes:

```powershell
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/config.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/minio.yaml
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/keycloak.yaml
kubectl apply -f k8s/services.yaml
```

See `docs/kubernetes.md` for image build, deployment, and port-forward details.

AWS infrastructure scaffolding lives in `infrastructure/terraform/envs/dev`. It provisions a cost-conscious EKS MVP, private S3 bucket, SES sender identity, IRSA roles, and ALB controller IAM. See `docs/aws-deployment.md`.

CI/CD is handled by GitHub Actions with Terraform bootstrap, pull request checks, and approved `dev` deployments. See `docs/cicd.md`.

## Verification Status

- Context tests pass for `api-service`, `video-service`, and `processing-service`.
- `docker compose config` validates.
- End-to-end MVP upload and processing was previously verified before the auth/storage expansion.
