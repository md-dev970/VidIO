package com.mddev.videoservice.controller;

import com.mddev.videoservice.dto.AdminOverviewResponse;
import com.mddev.videoservice.dto.PresignedUrlResponse;
import com.mddev.videoservice.dto.VideoResponse;
import com.mddev.videoservice.service.VideoService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminVideoController {

    private final VideoService videoService;

    public AdminVideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/videos")
    public List<VideoResponse> findAllVideos() {
        return videoService.findAllForAdmin();
    }

    @GetMapping("/videos/{id}")
    public VideoResponse findVideoById(@PathVariable UUID id) {
        return videoService.findByIdForAdmin(id);
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return videoService.overviewForAdmin();
    }

    @GetMapping("/videos/{id}/assets/{assetType}/url")
    public PresignedUrlResponse assetUrl(@PathVariable UUID id, @PathVariable String assetType) {
        return videoService.assetUrlForAdmin(id, assetType);
    }
}
