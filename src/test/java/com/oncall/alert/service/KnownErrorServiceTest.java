package com.oncall.alert.service;

import com.oncall.alert.model.ErrorSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnownErrorServiceTest {

    private KnownErrorService knownErrorService;

    @BeforeEach
    void setUp() {
        knownErrorService = new KnownErrorService();
        knownErrorService.loadKnownErrors();
    }

    @Test
    void enrichWithKnownErrorInfo_matchesByErrorCodeAndDocumentType() {
        ErrorSummary summary = ErrorSummary.builder()
                .errorCode("DOC_GEN_001")
                .documentType("INVOICE")
                .errorMessage("Template not found for id X")
                .build();

        knownErrorService.enrichWithKnownErrorInfo(summary);

        assertTrue(summary.isKnownError());
        assertTrue(summary.getKnownErrorNote().contains("IGNORABLE"));
    }

    @Test
    void enrichWithKnownErrorInfo_noMatchLeavesKnownFlagFalse() {
        ErrorSummary summary = ErrorSummary.builder()
                .errorCode("UNKNOWN_999")
                .documentType("OTHER")
                .errorMessage("Something completely new")
                .build();

        knownErrorService.enrichWithKnownErrorInfo(summary);

        assertFalse(summary.isKnownError());
    }
}
