package com.mddev.apiservice.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<String> handleDownstream(RestClientResponseException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(exception.getResponseBodyAsString());
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("timestamp", Instant.now(), "message", exception.getMessage()));
    }
}
