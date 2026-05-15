package com.mddev.videoservice.repository;

import com.mddev.videoservice.entity.VideoEntity;
import com.mddev.videoservice.entity.VideoStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<VideoEntity, UUID> {
    List<VideoEntity> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    Optional<VideoEntity> findByIdAndOwnerId(UUID id, String ownerId);

    long countByStatus(VideoStatus status);
}
