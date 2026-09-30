package com.oncall.alert.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertTriggerResponse {
    private String status;
    private String message;
    private int errorsFound;
    private String reportId;
}
