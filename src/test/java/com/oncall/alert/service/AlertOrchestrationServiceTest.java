package com.oncall.alert.service;

import com.oncall.alert.config.AppProperties;
import com.oncall.alert.model.AlertTriggerRequest;
import com.oncall.alert.model.AlertTriggerResponse;
import com.oncall.alert.model.ErrorLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertOrchestrationServiceTest {

    @Mock
    private OpenSearchService openSearchService;
    @Mock
    private KnownErrorService knownErrorService;
    @Mock
    private DeepSeekService deepSeekService;
    @Mock
    private EmailService emailService;
    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AlertOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        when(appProperties.getCooldownSeconds()).thenReturn(300);
        when(appProperties.getLogWindowMinutes()).thenReturn(15);
    }

    @Test
    void processAlert_returnsOkWhenNoErrors() {
        when(openSearchService.fetchErrorLogs(any(), any())).thenReturn(List.of());

        AlertTriggerRequest request = new AlertTriggerRequest();
        request.setSource("test");
        request.setOncallEmail("oncall@example.com");

        AlertTriggerResponse response = orchestrationService.processAlert(request);

        assertEquals("OK", response.getStatus());
        assertEquals(0, response.getErrorsFound());
        verify(emailService, never()).sendAlertReport(any(), any());
    }

    @Test
    void processAlert_sendsEmailWhenErrorsFound() {
        ErrorLogEntry entry = new ErrorLogEntry();
        entry.setErrorCode("DOC_GEN_002");
        entry.setDocumentType("INVOICE");
        entry.setErrorMessage("Timeout waiting for upstream");
        entry.setApi("/generate");
        entry.setTimestamp(Instant.now());
        entry.setTraceId("trace-1");

        when(openSearchService.fetchErrorLogs(any(), any())).thenReturn(List.of(entry));
        when(openSearchService.getIndexName()).thenReturn("application-logs");
        when(deepSeekService.getRemediation(any())).thenReturn("Restart upstream");

        AlertTriggerRequest request = new AlertTriggerRequest();
        request.setSource("xmatters");
        request.setOncallEmail("john@example.com");

        AlertTriggerResponse response = orchestrationService.processAlert(request);

        assertEquals("SENT", response.getStatus());
        assertEquals(1, response.getErrorsFound());
        verify(emailService).sendAlertReport(any(), any());
    }
}
