package com.oncall.alert.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a single error log document from OpenSearch.
 */
@Data
public class ErrorLogEntry {
    @JsonProperty("@timestamp")
    private Instant timestamp;

    private String message;

    @JsonProperty("attribute.error.code")
    private String errorCode;

    @JsonProperty("attribute.error.msg")
    private String errorMessage;

    @JsonProperty("documenttype")
    private String documentType;

    private String api;

    private String traceId;

    private String level;
}
