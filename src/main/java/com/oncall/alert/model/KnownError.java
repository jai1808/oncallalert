package com.oncall.alert.model;

import lombok.Data;

import java.util.List;

@Data
public class KnownError {
    private String errorCode;
    private String errorMessagePattern;
    private List<String> documentTypes;
    private String severity;   // "ignorable" | "warning" | "critical"
    private String note;
    private String resolution;
}
