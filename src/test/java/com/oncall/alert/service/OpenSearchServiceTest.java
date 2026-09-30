package com.oncall.alert.service;

import com.oncall.alert.config.AppProperties;
import com.oncall.alert.exception.OpenSearchQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenSearchServiceTest {

    @Mock
    private OpenSearchClient openSearchClient;
    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private OpenSearchService openSearchService;

    @Test
    void fetchErrorLogs_wrapsFailuresAsOpenSearchQueryException() throws Exception {
        ReflectionTestUtils.setField(openSearchService, "indexName", "application-logs");
        when(appProperties.getErrorMessagePrefix()).thenReturn("Error during Document Generation");
        when(openSearchClient.search(any(), any())).thenThrow(new RuntimeException("connection refused"));

        assertThrows(OpenSearchQueryException.class, () ->
                openSearchService.fetchErrorLogs(Instant.now().minusSeconds(900), Instant.now()));
    }
}
