package com.oncall.alert.model;

import lombok.Data;

@Data
public class AlertTriggerRequest {
    /** On-call person's email (from xMatters payload) */
    private String oncallEmail;
    /** Alert source identifier */
    private String source;
    /** Optional: specific incident ID */
    private String incidentId;
    /** Optional: custom message from xMatters */
    private String message;
}
