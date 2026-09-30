package com.oncall.alert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alert")
public class AppProperties {
    private int logWindowMinutes = 15;
    private String errorMessagePrefix = "Error during Document Generation";
    private int cooldownSeconds = 300;
    private String oncallDefaultEmail;
    private String fromEmail;
}
