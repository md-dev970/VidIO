variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project_name" {
  type    = string
  default = "vidio"
}

variable "cluster_name" {
  type    = string
  default = "vidio-dev"
}

variable "video_bucket_name" {
  type        = string
  description = "Globally unique S3 bucket name for VidIO video assets."
}

variable "route53_zone_name" {
  type        = string
  description = "Delegated public hosted zone for VidIO, for example vidio.domain.com."
}

variable "app_hostname" {
  type        = string
  description = "Public hostname for the Angular portal, for example vidio.domain.com."
}

variable "api_hostname" {
  type        = string
  description = "Public hostname for the API, for example api.vidio.domain.com."
}

variable "ses_email_identity" {
  type        = string
  description = "Email address to verify in SES for Keycloak verification emails."
}

variable "ses_domain_identity" {
  type        = string
  default     = ""
  description = "Domain to verify in SES. Defaults to route53_zone_name when empty."
}

variable "alb_dns_name" {
  type        = string
  default     = ""
  description = "ALB DNS name from kubectl get ingress. Leave empty until the ingress exists."
}

variable "alb_zone_id" {
  type        = string
  default     = ""
  description = "Canonical hosted zone ID of the ALB. Leave empty until the ingress exists."
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.small"]
}

variable "node_desired_size" {
  type    = number
  default = 3
}

variable "node_min_size" {
  type    = number
  default = 2
}

variable "node_max_size" {
  type    = number
  default = 3
}

variable "github_deploy_role_arn" {
  type        = string
  default     = ""
  description = "Optional GitHub Actions deploy role ARN to grant EKS cluster admin access."
}
