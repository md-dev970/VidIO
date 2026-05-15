package com.mddev.processingservice.controller;

import com.mddev.processingservice.dto.ProcessingJobResponse;
import com.mddev.processingservice.service.ProcessingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminProcessingController {

    private final ProcessingService processingService;

    public AdminProcessingController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    @GetMapping("/jobs")
    public List<ProcessingJobResponse> findAllJobs() {
        return processingService.findAllJobsForAdmin();
    }
}
