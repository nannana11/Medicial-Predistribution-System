package com.triage.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAiClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockAiClient client = new MockAiClient();

    @Test
    void shouldTreatOrdinaryCoughAsNonEmergencyRespiratoryCase() throws Exception {
        JsonNode result = analyze("我一直在咳嗽");

        assertEquals("呼吸内科", result.path("department").asText());
        assertEquals("普通", result.path("severity").asText());
    }

    @Test
    void shouldTreatBloodLossAsEmergency() throws Exception {
        JsonNode result = analyze("我一直在失血");

        assertEquals("急诊科", result.path("department").asText());
        assertEquals("紧急", result.path("severity").asText());
        assertTrue(result.path("advice").asText().contains("现场医护人员"));
        assertTrue(result.path("advice").asText().contains("急诊科"));
    }

    @Test
    void shouldTreatChestPainWithSweatingAsEmergency() throws Exception {
        JsonNode result = analyze("我胸口疼，还一直冒冷汗");

        assertEquals("急诊科", result.path("department").asText());
        assertEquals("紧急", result.path("severity").asText());
    }

    @Test
    void shouldRecommendGastroenterologyForDiarrhea() throws Exception {
        JsonNode result = analyze("我肚子疼，还拉肚子");

        assertEquals("消化内科", result.path("department").asText());
        assertEquals("普通", result.path("severity").asText());
    }

    @Test
    void shouldRequestMoreInformationForUnrecognizedQuestion() throws Exception {
        JsonNode result = analyze("你能告诉我今天星期几吗");

        assertEquals("暂无法判断", result.path("department").asText());
        assertEquals("待补充", result.path("severity").asText());
        assertTrue(result.path("advice").asText().contains("具体不适"));
    }

    private JsonNode analyze(String symptoms) throws Exception {
        return objectMapper.readTree(client.analyze(symptoms));
    }
}
