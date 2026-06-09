package com.example.triageclient.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriageResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseStructuredServerResponse() throws Exception {
        String json = """
                {
                  "success": true,
                  "recommendedDepartment": "呼吸内科",
                  "urgencyLevel": "medium",
                  "needEmergency": false,
                  "reply": "建议优先咨询呼吸内科。"
                }
                """;

        TriageResponse response = objectMapper.readValue(json, TriageResponse.class);

        assertTrue(response.isSuccess());
        assertEquals("呼吸内科", response.getRecommendedDepartment());
        assertEquals("medium", response.getUrgencyLevel());
        assertFalse(response.isNeedEmergency());
        assertEquals("建议优先咨询呼吸内科。", response.getReply());
    }

    @Test
    void shouldParseUnknownUrgencyResponse() throws Exception {
        String json = """
                {
                  "success": true,
                  "recommendedDepartment": "暂无法判断",
                  "urgencyLevel": "unknown",
                  "needEmergency": false,
                  "reply": "请补充具体症状。"
                }
                """;

        TriageResponse response = objectMapper.readValue(json, TriageResponse.class);

        assertEquals("暂无法判断", response.getRecommendedDepartment());
        assertEquals("unknown", response.getUrgencyLevel());
        assertFalse(response.isNeedEmergency());
    }
}
