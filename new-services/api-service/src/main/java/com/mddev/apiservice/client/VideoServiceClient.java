package com.mddev.apiservice.client;

import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class VideoServiceClient {

    private final RestClient restClient;

    public VideoServiceClient(@Value("${video-service.base-url}") String videoServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(videoServiceBaseUrl)
                .build();
    }

    public String upload(MultipartFile file) {
        try {
            var fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(contentType(file)));
            fileHeaders.setContentDisposition(ContentDisposition.formData()
                    .name("file")
                    .filename(file.getOriginalFilename())
                    .build());

            var body = new LinkedMultiValueMap<String, Object>();
            body.add("file", new HttpEntity<>(new MultipartFileResource(file), fileHeaders));

            return restClient.post()
                    .uri("/videos")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read uploaded file", exception);
        }
    }

    public String findById(UUID id) {
        return restClient.get()
                .uri("/videos/{id}", id)
                .retrieve()
                .body(String.class);
    }

    public String findAll() {
        return restClient.get()
                .uri("/videos")
                .retrieve()
                .body(String.class);
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
    }

    private static final class MultipartFileResource extends ByteArrayResource {

        private final String filename;

        private MultipartFileResource(MultipartFile file) throws IOException {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
