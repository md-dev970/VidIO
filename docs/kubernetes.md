# Kubernetes

The manifests in `k8s/` target Docker Desktop Kubernetes for local development. Docker Compose is still the fastest feedback loop; Kubernetes is for proving the platform shape.

## Build Local Images

Build the images into Docker Desktop's local image store:

```powershell
docker compose build api-service video-service processing-service admin-dashboard
docker image tag vidio-api-service:latest vidio-api-service:latest
docker image tag vidio-video-service:latest vidio-video-service:latest
docker image tag vidio-processing-service:latest vidio-processing-service:latest
docker image tag vidio-admin-dashboard:latest vidio-admin-dashboard:latest
```

The Kubernetes Deployments use `imagePullPolicy: IfNotPresent`, so Docker Desktop Kubernetes can use those local images.

## Apply Order

```powershell
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/config.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/minio.yaml
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/keycloak.yaml
kubectl apply -f k8s/services.yaml
```

Check rollout:

```powershell
kubectl get pods -n vidio
kubectl get svc -n vidio
```

## Port Forwarding

Use separate terminals:

```powershell
kubectl port-forward -n vidio svc/api-service 8081:8081
kubectl port-forward -n vidio svc/admin-dashboard 8088:80
kubectl port-forward -n vidio svc/keycloak 8089:8080
kubectl port-forward -n vidio svc/minio 9000:9000
kubectl port-forward -n vidio svc/minio 9001:9001
```

Then use:

```text
API:             http://localhost:8081
VidIO portal:     http://localhost:8088
Keycloak:        http://localhost:8089
MinIO console:   http://localhost:9001
```

## Notes

- The included Keycloak realm import creates the `vidio` realm and demo users.
- Self-registration is enabled. Local verification email requires SMTP values in `vidio-secrets`; Docker Compose uses Mailpit.
- Secrets are intentionally simple local-development values.
- Production TLS, external ingress, managed storage, and real secret management are out of scope for this phase.

## AWS Overlay

AWS-specific manifests live under `k8s/aws/` and intentionally omit MinIO. They expect the Terraform-created S3 bucket and IRSA roles:

```powershell
kubectl apply -k k8s/aws
```

Before applying, replace the placeholder bucket name, service account role ARNs, SMTP values, and image names with your Terraform/ECR values.
