variable "aws_region" {
  type    = string
  default = "us-west-2"
}

variable "project_name" {
  type    = string
  default = "vidio"
}

variable "github_owner" {
  type    = string
  default = "md-dev970"
}

variable "github_repo" {
  type    = string
  default = "VidIO"
}

variable "state_bucket_name" {
  type        = string
  description = "Globally unique S3 bucket name for Terraform remote state."
}

variable "lock_table_name" {
  type    = string
  default = "vidio-terraform-locks"
}

variable "deploy_role_name" {
  type    = string
  default = "vidio-github-deploy"
}

variable "create_github_oidc_provider" {
  type        = bool
  default     = true
  description = "Set to false if the GitHub Actions OIDC provider already exists in this AWS account."
}
