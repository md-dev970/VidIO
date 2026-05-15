package com.mddev.videoservice.controller;

import com.mddev.videoservice.dto.PresignedUrlResponse;
import com.mddev.videoservice.dto.VideoResponse;
import com.mddev.videoservice.security.AuthenticatedUser;
import com.mddev.videoservice.service.VideoService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public VideoResponse upload(@RequestPart("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        return videoService.upload(file, AuthenticatedUser.from(jwt));
    }

    @GetMapping("/{id}")
    public VideoResponse findById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return videoService.findById(id, AuthenticatedUser.from(jwt));
    }

    @GetMapping
    public List<VideoResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return videoService.findAll(AuthenticatedUser.from(jwt));
    }

    @GetMapping("/{id}/assets/{assetType}/url")
    public PresignedUrlResponse assetUrl(@PathVariable UUID id,
                                         @PathVariable String assetType,
                                         @AuthenticationPrincipal Jwt jwt) {
        return videoService.assetUrl(id, assetType, AuthenticatedUser.from(jwt));
    }
}
