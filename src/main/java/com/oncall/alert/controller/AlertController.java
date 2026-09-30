package com.oncall.alert.controller;

import com.oncall.alert.model.AlertTriggerRequest;
import com.oncall.alert.model.AlertTriggerResponse;
import com.oncall.alert.service.AlertOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertOrchestrationService orchestrationService;

    /**
     * Triggered by xMatters or any external alerting system.
     * Pipeline: OpenSearch → aggregate → known errors → DeepSeek → email.
     */
    @PostMapping("/trigger")
    public ResponseEntity<AlertTriggerResponse> triggerAlert(
            @RequestBody @Valid AlertTriggerRequest request) {
        log.info("Alert triggered from source={}, oncall={}",
                request.getSource(), request.getOncallEmail());

        AlertTriggerResponse response = orchestrationService.processAlert(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
