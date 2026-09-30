package com.oncall.alert.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DeepSeekRequest {
    private String model;
    private List<Message> messages;
    private int max_tokens;
    private double temperature;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
