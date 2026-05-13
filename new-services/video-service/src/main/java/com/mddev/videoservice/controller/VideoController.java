package com.mddev.videoservice.controller;

import com.mddev.videoservice.dto.VideoResponse;
import com.mddev.videoservice.service.VideoService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponse upload(@RequestPart("file") MultipartFile file) {
        return videoService.upload(file);
    }

    @GetMapping("/{id}")
    public VideoResponse findById(@PathVariable UUID id) {
        return videoService.findById(id);
    }

    @GetMapping
    public List<VideoResponse> findAll() {
        return videoService.findAll();
    }
}
