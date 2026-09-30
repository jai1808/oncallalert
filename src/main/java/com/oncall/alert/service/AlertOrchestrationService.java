package com.oncall.alert.service;

import com.oncall.alert.config.AppProperties;
import com.oncall.alert.model.AlertReport;
import com.oncall.alert.model.AlertTriggerRequest;
import com.oncall.alert.model.AlertTriggerResponse;
import com.oncall.alert.model.ErrorLogEntry;
import com.oncall.alert.model.ErrorSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertOrchestrationService {

    private final OpenSearchService openSearchService;
    private final KnownErrorService knownErrorService;
    private final DeepSeekService deepSeekService;
    private final EmailService emailService;
    private final AppProperties appProperties;

    private final Map<String, Instant> lastAlertTimes = new ConcurrentHashMap<>();

    public AlertTriggerResponse processAlert(AlertTriggerRequest request) {
        String reportId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String cooldownKey = Optional.ofNullable(request.getSource()).orElse("default");
        Instant lastAlert = lastAlertTimes.get(cooldownKey);
        if (lastAlert != null && lastAlert.plusSeconds(appProperties.getCooldownSeconds())
                .isAfter(Instant.now())) {
            log.warn("Alert cooldown active for source={}, skipping", cooldownKey);
            return AlertTriggerResponse.builder()
                    .status("SKIPPED")
                    .message("Cooldown active. Last alert was recent.")
                    .errorsFound(0)
                    .reportId(reportId)
                    .build();
        }

        try {
            Instant windowEnd = Instant.now();
            Instant windowStart = windowEnd.minusSeconds(
                    appProperties.getLogWindowMinutes() * 60L);

            log.info("[{}] Querying OpenSearch: {} to {}", reportId, windowStart, windowEnd);
            List<ErrorLogEntry> rawLogs = openSearchService.fetchErrorLogs(windowStart, windowEnd);

            if (rawLogs.isEmpty()) {
                log.info("[{}] No errors found in the last {} minutes",
                        reportId, appProperties.getLogWindowMinutes());
                return AlertTriggerResponse.builder()
                        .status("OK")
                        .message("No errors found in the time window.")
                        .errorsFound(0)
                        .reportId(reportId)
                        .build();
            }

            List<ErrorSummary> summaries = aggregateErrors(rawLogs);

            for (ErrorSummary summary : summaries) {
                knownErrorService.enrichWithKnownErrorInfo(summary);
            }

            for (ErrorSummary summary : summaries) {
                try {
                    String remediation = deepSeekService.getRemediation(summary);
                    summary.setRemediationSuggestion(remediation);
                } catch (Exception e) {
                    log.error("[{}] DeepSeek call failed for error code={}",
                            reportId, summary.getErrorCode(), e);
                    summary.setRemediationSuggestion(
                            "LLM remediation unavailable: " + e.getMessage());
                }
            }

            AlertReport report = AlertReport.builder()
                    .reportId(reportId)
                    .generatedAt(Instant.now())
                    .windowStart(windowStart)
                    .windowEnd(windowEnd)
                    .indexName(openSearchService.getIndexName())
                    .totalErrorCount(rawLogs.size())
                    .errorSummaries(summaries)
                    .build();

            String recipientEmail = Optional.ofNullable(request.getOncallEmail())
                    .filter(e -> !e.isBlank())
                    .orElse(appProperties.getOncallDefaultEmail());

            emailService.sendAlertReport(recipientEmail, report);
            lastAlertTimes.put(cooldownKey, Instant.now());

            log.info("[{}] Alert processed: {} errors, {} groups, emailed to {}",
                    reportId, rawLogs.size(), summaries.size(), recipientEmail);

            return AlertTriggerResponse.builder()
                    .status("SENT")
                    .message("Alert report emailed to " + recipientEmail)
                    .errorsFound(rawLogs.size())
                    .reportId(reportId)
                    .build();

        } catch (Exception e) {
            log.error("[{}] Alert processing failed", reportId, e);
            return AlertTriggerResponse.builder()
                    .status("ERROR")
                    .message("Processing failed: " + e.getMessage())
                    .errorsFound(0)
                    .reportId(reportId)
                    .build();
        }
    }

    private List<ErrorSummary> aggregateErrors(List<ErrorLogEntry> logs) {
        Map<String, List<ErrorLogEntry>> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        logEntry -> (logEntry.getErrorCode() != null ? logEntry.getErrorCode() : "UNKNOWN")
                                + "|"
                                + (logEntry.getDocumentType() != null ? logEntry.getDocumentType() : "UNKNOWN")
                ));

        return grouped.values().stream().map(group -> {
            ErrorLogEntry sample = group.get(0);
            return ErrorSummary.builder()
                    .errorCode(sample.getErrorCode())
                    .errorMessage(sample.getErrorMessage())
                    .documentType(sample.getDocumentType())
                    .api(sample.getApi())
                    .occurrenceCount(group.size())
                    .firstOccurrence(group.stream()
                            .map(ErrorLogEntry::getTimestamp)
                            .filter(Objects::nonNull)
                            .min(Instant::compareTo).orElse(null))
                    .lastOccurrence(group.stream()
                            .map(ErrorLogEntry::getTimestamp)
                            .filter(Objects::nonNull)
                            .max(Instant::compareTo).orElse(null))
                    .sampleTraceIds(group.stream()
                            .map(ErrorLogEntry::getTraceId)
                            .filter(Objects::nonNull)
                            .limit(5)
                            .collect(Collectors.toList()))
                    .build();
        }).sorted(Comparator.comparingInt(ErrorSummary::getOccurrenceCount).reversed())
                .collect(Collectors.toList());
    }
}
