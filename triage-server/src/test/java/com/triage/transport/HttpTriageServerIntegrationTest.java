package com.triage.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTriageServerIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private HttpTriageServer server;
    private Thread serverThread;
    private AtomicReference<Throwable> serverFailure;

    @BeforeEach
    void startServer() throws Exception {
        System.setProperty("triage.ai.mock", "true");
        server = new HttpTriageServer();
        serverFailure = new AtomicReference<>();
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Throwable throwable) {
                serverFailure.set(throwable);
            }
        }, "triage-server-integration-test");
        serverThread.start();

        waitUntilHealthy();
        assertNull(serverFailure.get(), "服务器启动失败");
    }

    @AfterEach
    void stopServer() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }
        if (serverThread != null) {
            serverThread.join(3000);
        }
        System.clearProperty("triage.ai.mock");
    }

    @Test
    void shouldReturnMediumUrgencyForOrdinaryCough() throws Exception {
        JsonNode json = submitSymptoms("我一直在咳嗽");

        assertTrue(json.path("success").asBoolean());
        assertEquals("呼吸内科", json.path("recommendedDepartment").asText());
        assertEquals("medium", json.path("urgencyLevel").asText());
        assertFalse(json.path("needEmergency").asBoolean());
        assertFalse(json.path("reply").asText().isBlank());
    }

    @Test
    void shouldReturnEmergencyForContinuousBloodLoss() throws Exception {
        JsonNode json = submitSymptoms("我一直在失血");

        assertTrue(json.path("success").asBoolean());
        assertEquals("急诊科", json.path("recommendedDepartment").asText());
        assertEquals("high", json.path("urgencyLevel").asText());
        assertTrue(json.path("needEmergency").asBoolean());
    }

    @Test
    void shouldRequestMoreInformationForUnknownInput() throws Exception {
        JsonNode json = submitSymptoms("你能介绍一下自己吗");

        assertTrue(json.path("success").asBoolean());
        assertEquals("暂无法判断", json.path("recommendedDepartment").asText());
        assertEquals("unknown", json.path("urgencyLevel").asText());
        assertFalse(json.path("needEmergency").asBoolean());
    }

    private JsonNode submitSymptoms(String message) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("message", message));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/triage/message"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private void waitUntilHealthy() throws Exception {
        URI uri = URI.create("http://localhost:8080/api/health");
        for (int attempt = 0; attempt < 20; attempt++) {
            if (serverFailure.get() != null) {
                throw new AssertionError("服务器启动失败", serverFailure.get());
            }
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(1))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                Thread.sleep(100);
            }
        }
        throw new AssertionError("等待服务器启动超时");
    }
}
