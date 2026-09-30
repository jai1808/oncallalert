package com.oncall.alert.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated view of errors grouped by error code + document type.
 */
@Data
@Builder
public class ErrorSummary {
    private String errorCode;
    private String errorMessage;
    private String documentType;
    private String api;
    private int occurrenceCount;
    private Instant firstOccurrence;
    private Instant lastOccurrence;
    private List<String> sampleTraceIds;

    /** Populated after known-error lookup */
    private boolean knownError;
    private String knownErrorNote;

    /** Populated after DeepSeek LLM call */
    private String remediationSuggestion;
}
