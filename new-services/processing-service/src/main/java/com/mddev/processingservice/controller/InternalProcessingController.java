package com.mddev.processingservice.controller;

import com.mddev.processingservice.dto.ManualProcessingRequest;
import com.mddev.processingservice.dto.ProcessingJobResponse;
import com.mddev.processingservice.service.ProcessingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalProcessingController {

    private final ProcessingService processingService;

    public InternalProcessingController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping("/process")
    public ProcessingJobResponse fakeProcess(@Valid @RequestBody ManualProcessingRequest request) {
        return processingService.fakeProcess(request);
    }
}
