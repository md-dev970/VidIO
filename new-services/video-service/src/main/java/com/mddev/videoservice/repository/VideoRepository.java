package com.mddev.videoservice.repository;

import com.mddev.videoservice.entity.VideoEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<VideoEntity, UUID> {
}
