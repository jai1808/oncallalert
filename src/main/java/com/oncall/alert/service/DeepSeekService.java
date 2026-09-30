package com.oncall.alert.service;

import com.oncall.alert.model.DeepSeekRequest;
import com.oncall.alert.model.DeepSeekResponse;
import com.oncall.alert.model.ErrorSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final WebClient deepSeekWebClient;

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.max-tokens}")
    private int maxTokens;

    @Value("${deepseek.temperature}")
    private double temperature;

    public String getRemediation(ErrorSummary summary) {
        String systemPrompt = """
                You are an expert on-call SRE assistant for a document generation microservice
                used by 5 million members. When given error details, provide:

                1. **Root Cause Analysis** — Most likely reason for this error.
                2. **Immediate Remediation** — Steps the on-call engineer should take right now.
                3. **Long-term Fix** — What the team should do to prevent recurrence.
                4. **Escalation Guidance** — When/who to escalate to if the fix doesn't work.

                Be concise and actionable. Use bullet points. Focus on practical steps.
                """;

        String userPrompt = String.format("""
                Error occurred in our Document Generation service. Details:

                - **Error Code**: %s
                - **Error Message**: %s
                - **Document Type**: %s
                - **Failed API**: %s
                - **Occurrences**: %d times in the last 15 minutes
                - **First Seen**: %s
                - **Last Seen**: %s
                %s

                Provide remediation suggestions.
                """,
                summary.getErrorCode(),
                summary.getErrorMessage(),
                summary.getDocumentType(),
                summary.getApi(),
                summary.getOccurrenceCount(),
                summary.getFirstOccurrence(),
                summary.getLastOccurrence(),
                summary.isKnownError()
                        ? "NOTE: This is a KNOWN error: " + summary.getKnownErrorNote()
                        : ""
        );

        DeepSeekRequest request = DeepSeekRequest.builder()
                .model(model)
                .max_tokens(maxTokens)
                .temperature(temperature)
                .messages(List.of(
                        DeepSeekRequest.Message.builder()
                                .role("system").content(systemPrompt).build(),
                        DeepSeekRequest.Message.builder()
                                .role("user").content(userPrompt).build()
                ))
                .build();

        log.info("Calling DeepSeek for error code={}, docType={}",
                summary.getErrorCode(), summary.getDocumentType());

        DeepSeekResponse response = deepSeekWebClient
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekResponse.class)
                .block();

        return response != null ? response.getFirstContent() : "No remediation available.";
    }
}
