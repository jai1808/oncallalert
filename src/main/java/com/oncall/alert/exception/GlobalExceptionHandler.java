package com.oncall.alert.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OpenSearchQueryException.class)
    public ResponseEntity<Map<String, Object>> handleOpenSearchError(OpenSearchQueryException ex) {
        log.error("OpenSearch query failed", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "OpenSearch query failed",
                "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error",
                "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                "timestamp", Instant.now().toString()
        ));
    }
}
