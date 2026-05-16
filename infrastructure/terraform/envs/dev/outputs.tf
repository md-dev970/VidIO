output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.main.endpoint
}

output "video_bucket_name" {
  value = aws_s3_bucket.videos.bucket
}

output "ecr_repository_urls" {
  value = {
    api_service        = aws_ecr_repository.api_service.repository_url
    video_service      = aws_ecr_repository.video_service.repository_url
    processing_service = aws_ecr_repository.processing_service.repository_url
    admin_dashboard    = aws_ecr_repository.admin_dashboard.repository_url
  }
}

output "ses_email_identity" {
  value = aws_ses_email_identity.sender.email
}

output "route53_zone_id" {
  value = aws_route53_zone.vidio.zone_id
}

output "route53_name_servers" {
  value = aws_route53_zone.vidio.name_servers
}

output "acm_certificate_arn" {
  value = aws_acm_certificate_validation.vidio.certificate_arn
}

output "ses_domain_identity" {
  value = aws_ses_domain_identity.vidio.domain
}

output "video_service_role_arn" {
  value = aws_iam_role.video_service.arn
}

output "processing_service_role_arn" {
  value = aws_iam_role.processing_service.arn
}

output "alb_controller_role_arn" {
  value = aws_iam_role.alb_controller.arn
}

output "ebs_csi_role_arn" {
  value = aws_iam_role.ebs_csi.arn
}
