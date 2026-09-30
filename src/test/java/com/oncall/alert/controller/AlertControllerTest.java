package com.oncall.alert.controller;

import com.oncall.alert.model.AlertTriggerRequest;
import com.oncall.alert.model.AlertTriggerResponse;
import com.oncall.alert.service.AlertOrchestrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertOrchestrationService orchestrationService;

    @Test
    void health_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void triggerAlert_returnsOrchestrationResponse() throws Exception {
        when(orchestrationService.processAlert(any(AlertTriggerRequest.class)))
                .thenReturn(AlertTriggerResponse.builder()
                        .status("SENT")
                        .message("Alert report emailed to john@example.com")
                        .errorsFound(5)
                        .reportId("A3F2B1C8")
                        .build());

        mockMvc.perform(post("/api/v1/alerts/trigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oncallEmail": "john@example.com",
                                  "source": "xmatters",
                                  "incidentId": "INC-12345",
                                  "message": "Document generation errors detected"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.errorsFound").value(5))
                .andExpect(jsonPath("$.reportId").value("A3F2B1C8"));
    }
}
