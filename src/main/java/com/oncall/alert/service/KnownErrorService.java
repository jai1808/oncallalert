package com.oncall.alert.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oncall.alert.model.ErrorSummary;
import com.oncall.alert.model.KnownError;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class KnownErrorService {

    private List<KnownError> knownErrors;

    @PostConstruct
    public void loadKnownErrors() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("known-errors.json").getInputStream();
            knownErrors = mapper.readValue(is, new TypeReference<>() {});
            log.info("Loaded {} known error definitions", knownErrors.size());
        } catch (Exception e) {
            log.error("Failed to load known-errors.json, continuing with empty list", e);
            knownErrors = List.of();
        }
    }

    public void enrichWithKnownErrorInfo(ErrorSummary summary) {
        Optional<KnownError> match = knownErrors.stream()
                .filter(ke -> matchesError(ke, summary))
                .findFirst();

        if (match.isPresent()) {
            KnownError ke = match.get();
            summary.setKnownError(true);
            summary.setKnownErrorNote(
                    String.format("[%s] %s — %s",
                            ke.getSeverity() != null ? ke.getSeverity().toUpperCase() : "UNKNOWN",
                            ke.getNote(),
                            ke.getResolution() != null ? ke.getResolution() : "No action needed.")
            );
            log.info("Error code={} matched known error: severity={}",
                    summary.getErrorCode(), ke.getSeverity());
        }
    }

    private boolean matchesError(KnownError ke, ErrorSummary summary) {
        if (ke.getErrorCode() != null && ke.getErrorCode().equals(summary.getErrorCode())) {
            if (ke.getDocumentTypes() != null && !ke.getDocumentTypes().isEmpty()) {
                return ke.getDocumentTypes().contains(summary.getDocumentType());
            }
            return true;
        }
        if (ke.getErrorMessagePattern() != null && summary.getErrorMessage() != null) {
            return summary.getErrorMessage().contains(ke.getErrorMessagePattern());
        }
        return false;
    }
}
