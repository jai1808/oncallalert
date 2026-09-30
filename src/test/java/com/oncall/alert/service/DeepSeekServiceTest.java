package com.oncall.alert.service;

import com.oncall.alert.model.DeepSeekResponse;
import com.oncall.alert.model.ErrorSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeepSeekServiceTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private DeepSeekService deepSeekService;

    @BeforeEach
    void setUp() {
        deepSeekService = new DeepSeekService(webClient);
        ReflectionTestUtils.setField(deepSeekService, "model", "deepseek-chat");
        ReflectionTestUtils.setField(deepSeekService, "maxTokens", 2048);
        ReflectionTestUtils.setField(deepSeekService, "temperature", 0.3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRemediation_returnsFirstChoiceContent() {
        DeepSeekResponse.Message message = new DeepSeekResponse.Message();
        message.setContent("Check upstream health");
        DeepSeekResponse.Choice choice = new DeepSeekResponse.Choice();
        choice.setMessage(message);
        DeepSeekResponse llmResponse = new DeepSeekResponse();
        llmResponse.setChoices(List.of(choice));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ArgumentMatchers.<Class<DeepSeekResponse>>any()))
                .thenReturn(Mono.just(llmResponse));

        ErrorSummary summary = ErrorSummary.builder()
                .errorCode("DOC_GEN_002")
                .errorMessage("Timeout")
                .documentType("INVOICE")
                .api("/generate")
                .occurrenceCount(3)
                .firstOccurrence(Instant.now().minusSeconds(60))
                .lastOccurrence(Instant.now())
                .build();

        String result = deepSeekService.getRemediation(summary);

        assertEquals("Check upstream health", result);
    }
}
