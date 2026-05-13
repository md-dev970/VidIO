package com.mddev.apiservice.controller;

import com.mddev.apiservice.client.VideoServiceClient;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public String upload(@RequestPart("file") MultipartFile file) {
        return videoServiceClient.upload(file);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String findById(@PathVariable UUID id) {
        return videoServiceClient.findById(id);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String findAll() {
        return videoServiceClient.findAll();
    }
}
