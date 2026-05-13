package com.mddev.videoservice.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(exception.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<Map<String, Object>> handleStorage(StorageException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(exception.getMessage()));
    }

    private Map<String, Object> error(String message) {
        return Map.of("timestamp", Instant.now(), "message", message);
    }
}
