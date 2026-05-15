package com.mddev.apiservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProcessingServiceClient {

    private final RestClient restClient;

    public ProcessingServiceClient(@Value("${processing-service.base-url}") String processingServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(processingServiceBaseUrl)
                .build();
    }

    public String findAllJobsForAdmin(String authorizationHeader) {
        return restClient.get()
                .uri("/admin/jobs")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(String.class);
    }
}
