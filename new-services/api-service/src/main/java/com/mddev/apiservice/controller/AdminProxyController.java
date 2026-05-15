package com.mddev.apiservice.controller;

import com.mddev.apiservice.client.ProcessingServiceClient;
import com.mddev.apiservice.client.VideoServiceClient;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminProxyController {

    private final VideoServiceClient videoServiceClient;
    private final ProcessingServiceClient processingServiceClient;

    public AdminProxyController(VideoServiceClient videoServiceClient,
                                ProcessingServiceClient processingServiceClient) {
        this.videoServiceClient = videoServiceClient;
        this.processingServiceClient = processingServiceClient;
    }

    @GetMapping(value = "/videos", produces = MediaType.APPLICATION_JSON_VALUE)
    public String findAllVideos(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.findAllForAdmin(authorizationHeader);
    }

    @GetMapping(value = "/videos/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String findVideoById(@PathVariable UUID id,
                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.findByIdForAdmin(id, authorizationHeader);
    }

    @GetMapping(value = "/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    public String findAllJobs(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return processingServiceClient.findAllJobsForAdmin(authorizationHeader);
    }

    @GetMapping(value = "/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public String overview(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.overviewForAdmin(authorizationHeader);
    }

    @GetMapping(value = "/videos/{id}/assets/{assetType}/url", produces = MediaType.APPLICATION_JSON_VALUE)
    public String assetUrl(@PathVariable UUID id,
                           @PathVariable String assetType,
                           @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return videoServiceClient.assetUrlForAdmin(id, assetType, authorizationHeader);
    }
}
