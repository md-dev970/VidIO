# VidIO CI/CD Guide

This guide sets up GitHub Actions deployments for the `md-dev970/VidIO` repository.

The intended flow is:

1. Pull requests run tests, Docker build checks, Terraform validation, and Kubernetes manifest rendering.
2. Pull requests that touch Terraform also run a dev Terraform plan.
3. Merges to `main` run a Terraform plan.
4. GitHub pauses at the protected `dev` environment.
5. After approval, GitHub applies Terraform, builds/pushes images, deploys to EKS, and validates the public endpoints.

## 1. Bootstrap Terraform State And GitHub AWS Role

The bootstrap stack is applied once from your machine using your AWS credentials.

```powershell
Copy-Item infrastructure/terraform/bootstrap/terraform.tfvars.example infrastructure/terraform/bootstrap/terraform.tfvars
```

Edit:

```text
infrastructure/terraform/bootstrap/terraform.tfvars
```

Use:

```hcl
aws_region        = "us-west-2"
project_name      = "vidio"
github_owner      = "md-dev970"
github_repo       = "VidIO"
state_bucket_name = "us-west-2-md-dev970-vidio-terraform-state"
lock_table_name   = "vidio-terraform-locks"
deploy_role_name  = "vidio-github-deploy"
```

Apply:

```powershell
terraform -chdir=infrastructure/terraform/bootstrap init
terraform -chdir=infrastructure/terraform/bootstrap apply
terraform -chdir=infrastructure/terraform/bootstrap output
```

Save this output for GitHub:

```powershell
terraform -chdir=infrastructure/terraform/bootstrap output -raw github_deploy_role_arn
```

If AWS already has a GitHub OIDC provider, set this before applying:

```hcl
create_github_oidc_provider = false
```

## 2. Migrate Dev Terraform To Remote State

Add the bootstrap deploy role to dev Terraform variables:

```powershell
terraform -chdir=infrastructure/terraform/bootstrap output -raw github_deploy_role_arn
```

Set this in `infrastructure/terraform/envs/dev/terraform.tfvars`:

```hcl
github_deploy_role_arn = "arn:aws:iam::<account-id>:role/vidio-github-deploy"
```

Then migrate state:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev init -migrate-state
terraform -chdir=infrastructure/terraform/envs/dev plan
terraform -chdir=infrastructure/terraform/envs/dev apply
```

The apply grants the GitHub deploy role Kubernetes admin access through an EKS access entry.

If the four ECR repositories were created manually before Terraform managed them, import them once before the first apply:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev import aws_ecr_repository.api_service vidio-api-service
terraform -chdir=infrastructure/terraform/envs/dev import aws_ecr_repository.video_service vidio-video-service
terraform -chdir=infrastructure/terraform/envs/dev import aws_ecr_repository.processing_service vidio-processing-service
terraform -chdir=infrastructure/terraform/envs/dev import aws_ecr_repository.admin_dashboard vidio-admin-dashboard
```

If EKS rejects `aws_eks_access_entry.github_deploy` because the cluster authentication mode is not API-enabled yet, apply the cluster auth-mode update first:

```powershell
terraform -chdir=infrastructure/terraform/envs/dev apply -target=aws_eks_cluster.main
terraform -chdir=infrastructure/terraform/envs/dev apply
```

## 3. GitHub Dev Environment

Create a GitHub Environment named:

```text
dev
```

Enable required reviewers so deployments require approval.

Restrict deployment branches to:

```text
main
```

The deploy workflow runs Terraform plan and apply after the `dev` approval. Keep deployment values on the `dev` environment, not repository-wide, unless another workflow explicitly needs them.

Add these `dev` environment variables:

```text
AWS_REGION=us-west-2
AWS_ACCOUNT_ID=202197228322
EKS_CLUSTER_NAME=vidio-dev
APP_HOSTNAME=vidio.md-dev970.com
API_HOSTNAME=api.vidio.md-dev970.com
S3_BUCKET=us-west-2-md-dev970-vidio-dev-bucket
ROUTE53_ZONE_NAME=vidio.md-dev970.com
SES_EMAIL_IDENTITY=no-reply@vidio.md-dev970.com
SES_DOMAIN_IDENTITY=vidio.md-dev970.com
ALB_DNS_NAME=<current ALB DNS name from kubectl get ingress -n vidio>
ALB_ZONE_ID=<ALB canonical hosted zone id, for us-west-2 usually Z1H1FL5HABSF5>
KEYCLOAK_ADMIN=admin
VIDIO_SMTP_HOST=smtp-relay.brevo.com
VIDIO_SMTP_PORT=587
VIDIO_SMTP_FROM=no-reply@vidio.md-dev970.com
VIDIO_SMTP_AUTH=true
VIDIO_SMTP_STARTTLS=true
S3_REGION=us-west-2
S3_ENDPOINT=
S3_PUBLIC_ENDPOINT=
S3_PATH_STYLE_ACCESS=false
S3_PRESIGNED_URL_EXPIRATION_MINUTES=10
```

Add these `dev` environment secrets:

```text
AWS_DEPLOY_ROLE_ARN=<bootstrap github_deploy_role_arn output>
POSTGRES_PASSWORD
KEYCLOAK_ADMIN_PASSWORD
VIDIO_SMTP_USER
VIDIO_SMTP_PASSWORD
```

## 4. Branch Protection

For `main`, enable:

```text
Require a pull request before merging
Require status checks to pass before merging
Require branches to be up to date before merging
Block force pushes
```

Recommended required checks:

```text
Backend tests
Frontend build
Docker build checks
Terraform and Kubernetes validation
```

If GitHub shows matrix-expanded check names, select all three backend service test jobs and all four Docker build jobs.

## 5. Runtime Kubernetes Config

Do not commit:

```text
k8s/aws/config.yaml
```

It is ignored because it can contain real secrets. The deploy workflow generates it from GitHub Environment secrets.

For manual deploys, copy:

```powershell
Copy-Item k8s/aws/config.example.yaml k8s/aws/config.yaml
```

Then fill local values and apply:

```powershell
kubectl apply -k k8s/aws
```

## 6. Deployment Validation

The deploy workflow validates:

```text
api-service rollout
video-service rollout
processing-service rollout
admin-dashboard rollout
keycloak rollout
service endpoints
https://api.vidio.md-dev970.com/health
https://vidio.md-dev970.com
CORS preflight for POST /api/videos
```

An authenticated upload smoke test is intentionally manual for now. Add it later only after creating a dedicated CI test user and storing its credentials safely.
