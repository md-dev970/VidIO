# AWS Deployment Guide

This guide deploys VidIO from the current local/Kubernetes state to AWS with:

- `https://vidio.domain.com` for the Angular portal and Keycloak login routes.
- `https://api.vidio.domain.com` for API traffic.
- Route 53 managing only the delegated `vidio.domain.com` subdomain.
- Spaceship continuing to manage the root `domain.com`.
- EKS for the application workloads.
- S3 for video assets.
- SES for Keycloak signup verification email.
- ACM for HTTPS.
- ALB Ingress for public traffic.

Replace `domain.com` with your real root domain throughout.

## 1. Local Prerequisites

Install and authenticate:

- AWS CLI
- Docker Desktop
- Terraform
- `kubectl`
- Helm

Check AWS access:

```powershell
aws sts get-caller-identity
aws configure get region
```

Set local variables:

```powershell
$AWS_REGION="us-east-1"
$ROOT_DOMAIN="domain.com"
$ZONE_NAME="vidio.domain.com"
$APP_HOST="vidio.domain.com"
$API_HOST="api.vidio.domain.com"
$CLUSTER_NAME="vidio-dev"
$PROJECT="vidio"
$ACCOUNT_ID=(aws sts get-caller-identity --query Account --output text)
$ECR="$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
```

## 2. Create Terraform Variables

Copy the example:

```powershell
Copy-Item infrastructure/terraform/envs/dev/terraform.tfvars.example infrastructure/terraform/envs/dev/terraform.tfvars
```

Edit `infrastructure/terraform/envs/dev/terraform.tfvars`:

```hcl
aws_region          = "us-east-1"
project_name        = "vidio"
cluster_name        = "vidio-dev"
video_bucket_name   = "vidio-domain-com-videos-dev"
route53_zone_name   = "vidio.domain.com"
app_hostname        = "vidio.domain.com"
api_hostname        = "api.vidio.domain.com"
ses_email_identity  = "no-reply@vidio.domain.com"
ses_domain_identity = "vidio.domain.com"

# Fill these after the ALB exists.
alb_dns_name = ""
alb_zone_id  = ""

node_instance_types = ["t3.small"]
node_desired_size   = 3
node_min_size       = 2
node_max_size       = 3
```

The bucket name must be globally unique. If this one is taken, add your initials or account id.

## 3. Create The Route 53 Subdomain Zone First

Because your root domain is managed by Spaceship, first create only the delegated subdomain hosted zone:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev init
terraform -chdir=infrastructure/terraform/envs/dev apply -target=aws_route53_zone.vidio
```

Get the Route 53 nameservers:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev output route53_name_servers
```

In Spaceship DNS for `domain.com`, add an `NS` record:

```text
Type: NS
Host/Name: vidio
Value: the 4 Route 53 nameservers from terraform output
TTL: automatic or 300
```

This delegates only `vidio.domain.com` to AWS. It does not move the whole root domain.

Wait for delegation:

```powershell
nslookup -type=NS vidio.domain.com
```

The answer should show the Route 53 nameservers.

## 4. Provision AWS Infrastructure

Now run the full Terraform apply:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev fmt
terraform -chdir=infrastructure/terraform/envs/dev validate
terraform -chdir=infrastructure/terraform/envs/dev plan
terraform -chdir=infrastructure/terraform/envs/dev apply
```

Terraform provisions:

- VPC and public subnets
- EKS cluster and managed node group
- EBS CSI add-on
- S3 bucket for video assets
- Route 53 hosted zone records for ACM and SES
- ACM certificate for `vidio.domain.com` and `api.vidio.domain.com`
- SES domain identity, DKIM records, and sender email identity
- IRSA roles for `video-service` and `processing-service`
- IAM role/policy for AWS Load Balancer Controller

Save outputs:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev output
```

You need:

```text
video_bucket_name
route53_name_servers
acm_certificate_arn
video_service_role_arn
processing_service_role_arn
alb_controller_role_arn
```

If ACM or SES validation is slow, wait a few minutes and run `terraform apply` again.

## 5. SES Production Access

SES may start in sandbox mode. In sandbox mode, verification emails can only be sent to verified recipients.

For real public signup:

1. Open AWS SES in `us-east-1`.
2. Go to Account dashboard.
3. Request production access.
4. Use case: transactional account verification emails for VidIO.

SMTP details for Keycloak:

```text
SMTP host: email-smtp.us-east-1.amazonaws.com
SMTP port: 587
From: no-reply@vidio.domain.com
SMTP username: generated in SES
SMTP password: generated in SES
```

Create SMTP credentials from:

```text
SES -> SMTP settings -> Create SMTP credentials
```

## 6. Connect kubectl To EKS

```powershell
aws eks update-kubeconfig --region $AWS_REGION --name $CLUSTER_NAME
kubectl get nodes
```

Wait until nodes are `Ready`.

## 7. Create ECR Repositories

```powershell
aws ecr create-repository --repository-name vidio-api-service --region $AWS_REGION
aws ecr create-repository --repository-name vidio-video-service --region $AWS_REGION
aws ecr create-repository --repository-name vidio-processing-service --region $AWS_REGION
aws ecr create-repository --repository-name vidio-admin-dashboard --region $AWS_REGION
```

If a repository already exists, ignore the `RepositoryAlreadyExistsException`.

Login Docker to ECR:

```powershell
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR
```

## 8. Configure Frontend Public URLs

The local portal uses localhost. For AWS, edit `admin-dashboard/src/main.ts`:

```ts
const apiBaseUrl = isLocalBrowser ? 'http://localhost:8081/api' : 'https://api.vidio.domain.com/api';
const keycloakUrl = isLocalBrowser ? 'http://localhost:8089' : 'https://vidio.domain.com';
```

Replace with your real domain if different.

## 9. Build And Push Images

```powershell
docker build -t vidio-api-service ./new-services/api-service
docker build -t vidio-video-service ./new-services/video-service
docker build -t vidio-processing-service ./new-services/processing-service
docker build -t vidio-admin-dashboard ./admin-dashboard
```

Tag:

```powershell
docker tag vidio-api-service:latest "$ECR/vidio-api-service:latest"
docker tag vidio-video-service:latest "$ECR/vidio-video-service:latest"
docker tag vidio-processing-service:latest "$ECR/vidio-processing-service:latest"
docker tag vidio-admin-dashboard:latest "$ECR/vidio-admin-dashboard:latest"
```

Push:

```powershell
docker push "$ECR/vidio-api-service:latest"
docker push "$ECR/vidio-video-service:latest"
docker push "$ECR/vidio-processing-service:latest"
docker push "$ECR/vidio-admin-dashboard:latest"
```

## 10. Install AWS Load Balancer Controller

Get values:

```powershell
$ALB_ROLE_ARN=(terraform -chdir=infrastructure/terraform/envs/dev output -raw alb_controller_role_arn)
$VPC_ID=(aws eks describe-cluster --name $CLUSTER_NAME --region $AWS_REGION --query "cluster.resourcesVpcConfig.vpcId" --output text)
```

Install:

```powershell
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller `
  -n kube-system `
  --set clusterName=$CLUSTER_NAME `
  --set region=$AWS_REGION `
  --set vpcId=$VPC_ID `
  --set serviceAccount.create=true `
  --set serviceAccount.name=aws-load-balancer-controller `
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=$ALB_ROLE_ARN
```

Verify:

```powershell
kubectl get deployment -n kube-system aws-load-balancer-controller
```

## 11. Configure AWS Kubernetes Files

Edit `k8s/aws/config.yaml`:

```yaml
S3_REGION: us-east-1
S3_BUCKET: <terraform video_bucket_name>
S3_ENDPOINT: ""
S3_PUBLIC_ENDPOINT: ""
S3_ACCESS_KEY: ""
S3_SECRET_KEY: ""
S3_PATH_STYLE_ACCESS: "false"
S3_PRESIGNED_URL_EXPIRATION_MINUTES: "10"

VIDIO_SMTP_HOST: email-smtp.us-east-1.amazonaws.com
VIDIO_SMTP_PORT: "587"
VIDIO_SMTP_FROM: no-reply@vidio.domain.com
VIDIO_SMTP_USER: <ses-smtp-username>
VIDIO_SMTP_PASSWORD: <ses-smtp-password>
VIDIO_SMTP_AUTH: "true"
VIDIO_SMTP_STARTTLS: "true"
```

Edit `k8s/aws/serviceaccounts.yaml`:

```yaml
eks.amazonaws.com/role-arn: <video_service_role_arn>
eks.amazonaws.com/role-arn: <processing_service_role_arn>
```

Edit image names in `k8s/aws/services.yaml`:

```text
<account>.dkr.ecr.us-east-1.amazonaws.com/vidio-api-service:latest
<account>.dkr.ecr.us-east-1.amazonaws.com/vidio-video-service:latest
<account>.dkr.ecr.us-east-1.amazonaws.com/vidio-processing-service:latest
<account>.dkr.ecr.us-east-1.amazonaws.com/vidio-admin-dashboard:latest
```

Edit `k8s/aws/ingress.yaml`:

- Replace `replace-with-terraform-acm-certificate-arn` with Terraform output `acm_certificate_arn`.
- Replace `vidio.domain.com` and `api.vidio.domain.com` if your actual hostnames differ.

Edit `k8s/aws/keycloak.yaml` if needed so the dashboard client contains:

```json
"redirectUris": [
  "https://vidio.domain.com/*",
  "http://localhost:8088/*",
  "http://localhost:4200/*"
],
"webOrigins": [
  "https://vidio.domain.com",
  "https://api.vidio.domain.com",
  "+"
]
```

## 12. Deploy VidIO To EKS

```powershell
kubectl apply -k k8s/aws
```

Watch rollout:

```powershell
kubectl get pods -n vidio -w
```

Check services and ingress:

```powershell
kubectl get svc -n vidio
kubectl get ingress -n vidio
```

Wait until ingress has an address like:

```text
k8s-vidio-xxxxxxxx.us-east-1.elb.amazonaws.com
```

## 13. Create Route 53 ALB Alias Records

Get the ALB DNS name:

```powershell
$ALB_DNS=(kubectl get ingress vidio -n vidio -o jsonpath="{.status.loadBalancer.ingress[0].hostname}")
$ALB_DNS
```

Get the ALB canonical hosted zone id:

```powershell
$ALB_NAME=($ALB_DNS -split "\.")[0]
$ALB_ZONE_ID=(aws elbv2 describe-load-balancers --region $AWS_REGION --query "LoadBalancers[?DNSName=='$ALB_DNS'].CanonicalHostedZoneId" --output text)
$ALB_ZONE_ID
```

Edit `infrastructure/terraform/envs/dev/terraform.tfvars`:

```hcl
alb_dns_name = "k8s-vidio-xxxxxxxx.us-east-1.elb.amazonaws.com"
alb_zone_id  = "Zxxxxxxxxxxxxx"
```

Apply Terraform again:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev apply
```

Terraform now creates Route 53 alias records:

```text
vidio.domain.com      -> ALB
api.vidio.domain.com  -> ALB
```

Wait for DNS:

```powershell
nslookup vidio.domain.com
nslookup api.vidio.domain.com
```

## 14. Verify Public Deployment

API health:

```powershell
curl.exe -i https://api.vidio.domain.com/health
```

Open:

```text
https://vidio.domain.com
```

Test:

1. Sign up with a real email address.
2. Open the verification email from SES.
3. Log in.
4. Upload a small `.mp4`.
5. Wait for status `COMPLETED`.
6. Open original, thumbnail, and processed assets.

Check S3:

```powershell
aws s3 ls s3://<video_bucket_name>/ --recursive
```

Check logs:

```powershell
kubectl logs -n vidio deployment/api-service
kubectl logs -n vidio deployment/video-service
kubectl logs -n vidio deployment/processing-service
kubectl logs -n vidio statefulset/kafka
```

## 15. Admin Access

Keycloak admin:

```text
https://vidio.domain.com/admin
```

Initial local/dev admin credentials:

```text
admin / admin
```

Change them before sharing the deployment.

## 16. Common Fixes

If `https://vidio.domain.com` does not resolve:

- Confirm Spaceship has an `NS` record for host `vidio`.
- Confirm it points to the Route 53 hosted zone nameservers.
- Run `nslookup -type=NS vidio.domain.com`.

If HTTPS fails:

- Confirm ACM certificate is `Issued`.
- Confirm `k8s/aws/ingress.yaml` has the correct certificate ARN.
- Confirm the ALB listener has HTTPS 443.

If login redirects fail:

- Check Keycloak client redirect URIs.
- Check frontend `keycloakUrl`.
- Check `/realms` and `/resources` route to Keycloak through the ALB.

If API returns `401`:

- Keep `KEYCLOAK_JWK_SET_URI` internal:
  ```text
  http://keycloak:8080/realms/vidio/protocol/openid-connect/certs
  ```

If presigned S3 URLs fail:

- Ensure `S3_ENDPOINT`, `S3_PUBLIC_ENDPOINT`, `S3_ACCESS_KEY`, and `S3_SECRET_KEY` are empty in AWS.
- Ensure `S3_PATH_STYLE_ACCESS` is `"false"`.
- Check IRSA role annotations on service accounts.

## 17. Tear Down

Delete Kubernetes workloads:

```powershell
kubectl delete -k k8s/aws
```

Empty S3 bucket:

```powershell
aws s3 rm s3://<video_bucket_name> --recursive
```

Destroy Terraform:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev destroy
```

Remove the `vidio` NS record from Spaceship if you no longer want to delegate the subdomain.
