package com.mddev.processingservice.repository;

import com.mddev.processingservice.entity.ProcessingJobEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJobEntity, UUID> {
    List<ProcessingJobEntity> findAllByOrderByCreatedAtDesc();
}
