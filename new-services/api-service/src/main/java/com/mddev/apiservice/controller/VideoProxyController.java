package com.mddev.apiservice.controller;

import com.mddev.apiservice.client.VideoServiceClient;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
public class VideoProxyController {

    private final VideoServiceClient videoServiceClient;

    public VideoProxyController(VideoServiceClient videoServiceClient) {
        this.videoServiceClient = videoServiceClient;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public String upload(@RequestPart("file") MultipartFile file,
                         @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.upload(file, authorizationHeader);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String findById(@PathVariable UUID id,
                           @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.findById(id, authorizationHeader);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String findAll(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.findAll(authorizationHeader);
    }

    @GetMapping(value = "/{id}/assets/{assetType}/url", produces = MediaType.APPLICATION_JSON_VALUE)
    public String assetUrl(@PathVariable UUID id,
                           @PathVariable String assetType,
                           @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.assetUrl(id, assetType, authorizationHeader);
    }
}
