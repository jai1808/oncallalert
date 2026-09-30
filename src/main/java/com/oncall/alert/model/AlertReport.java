package com.oncall.alert.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AlertReport {
    private String reportId;
    private Instant generatedAt;
    private Instant windowStart;
    private Instant windowEnd;
    private String indexName;
    private int totalErrorCount;
    private List<ErrorSummary> errorSummaries;
}
