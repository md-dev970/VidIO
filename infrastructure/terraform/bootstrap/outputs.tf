output "state_bucket_name" {
  value = aws_s3_bucket.terraform_state.bucket
}

output "lock_table_name" {
  value = aws_dynamodb_table.terraform_locks.name
}

output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}

output "backend_config" {
  value = {
    bucket         = aws_s3_bucket.terraform_state.bucket
    key            = "envs/dev/terraform.tfstate"
    region         = var.aws_region
    dynamodb_table = aws_dynamodb_table.terraform_locks.name
    encrypt        = true
  }
}
