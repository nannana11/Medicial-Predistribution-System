package com.triage.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.triage.ai.AiServiceException;
import com.triage.config.Config;
import com.triage.service.TriageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 第一阶段 HTTP/JSON 服务端。
 */
public class HttpTriageServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpTriageServer.class);
    private static final int MAX_REQUEST_BYTES = 16 * 1024;

    private final Config config = Config.getInstance();
    private final TriageService triageService = new TriageService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private HttpServer server;
    private ExecutorService executor;

    public void start() throws IOException, InterruptedException {
        int port = config.getServerPort();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(Math.max(4,
                Runtime.getRuntime().availableProcessors()));
        server.setExecutor(executor);
        server.createContext("/api/triage/message", this::handleTriage);
        server.createContext("/api/health", this::handleHealth);
        server.start();

        logger.info("==========================================");
        logger.info("  HTTP 服务端启动成功，等待分机连接...");
        logger.info("  地址: http://0.0.0.0:{}", port);
        logger.info("  分诊接口: POST http://0.0.0.0:{}/api/triage/message", port);
        logger.info("==========================================");
        shutdownLatch.await();
    }

    public void shutdown() {
        if (server != null) {
            server.stop(1);
            server = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        shutdownLatch.countDown();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        String clientIp = getClientIp(exchange);
        logger.info("✦ 分机连接建立 [{}] → GET /api/health", clientIp);

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorResponse("请求方法不支持"));
            return;
        }
        sendJson(exchange, 200, Map.of(
                "status", "UP",
                "aiMode", config.isMockMode() ? "mock" : "deepseek"));
    }

    private void handleTriage(HttpExchange exchange) throws IOException {
        String clientIp = getClientIp(exchange);

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorResponse("请使用 POST 方法提交症状"));
            return;
        }

        try {
            byte[] requestBytes = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
            if (requestBytes.length > MAX_REQUEST_BYTES) {
                sendJson(exchange, 413, errorResponse("请求内容过长"));
                return;
            }

            JsonNode request = objectMapper.readTree(requestBytes);
            String message = request.path("message").asText("").trim();
            if (message.isEmpty()) {
                sendJson(exchange, 400, errorResponse("症状描述不能为空"));
                return;
            }
            if (message.length() > 1000) {
                sendJson(exchange, 400, errorResponse("症状描述不能超过 1000 个字符"));
                return;
            }

            logger.info("✦ 分机 [{}] 提交症状: {}", clientIp, message);
            String aiResult = triageService.analyzeSymptoms(message);
            logger.info("✦ 已向分机 [{}] 返回分诊结果", clientIp);
            sendJson(exchange, 200, toClientResponse(aiResult));
        } catch (JsonProcessingException exception) {
            logger.warn("客户端 JSON 格式错误", exception);
            sendJson(exchange, 400, errorResponse("请求 JSON 格式错误"));
        } catch (AiServiceException exception) {
            logger.error("AI 服务处理失败: {}", exception.getMessage(), exception);
            sendJson(exchange, 503, errorResponse(toUserMessage(exception)));
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, errorResponse(exception.getMessage()));
        } catch (Exception exception) {
            logger.error("处理分诊请求失败", exception);
            sendJson(exchange, 500, errorResponse("服务器暂时无法处理请求，请稍后重试"));
        }
    }

    private Map<String, Object> toClientResponse(String aiResult) throws JsonProcessingException {
        JsonNode result = objectMapper.readTree(stripMarkdownCodeFence(aiResult));
        String department = result.path("department").asText("全科");
        String severity = result.path("severity").asText("普通");
        String advice = result.path("advice").asText("建议咨询现场医护人员。");
        boolean emergency = isEmergency(severity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("recommendedDepartment", department);
        response.put("urgencyLevel", toUrgencyLevel(severity));
        response.put("needEmergency", emergency);
        response.put("reply", advice);
        return response;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("recommendedDepartment", "");
        response.put("urgencyLevel", "");
        response.put("needEmergency", false);
        response.put("reply", message);
        return response;
    }

    private boolean isEmergency(String severity) {
        String normalized = severity.toLowerCase(Locale.ROOT);
        return normalized.contains("紧急")
                && !normalized.contains("非紧急")
                || normalized.contains("emergency")
                || normalized.contains("critical")
                || normalized.contains("high");
    }

    private String toUrgencyLevel(String severity) {
        String normalized = severity.toLowerCase(Locale.ROOT);
        if (isEmergency(severity)) {
            return "high";
        }
        if (normalized.contains("非紧急") || normalized.contains("low")) {
            return "low";
        }
        if (normalized.contains("待补充") || normalized.contains("unknown")) {
            return "unknown";
        }
        return "medium";
    }

    private String stripMarkdownCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String toUserMessage(AiServiceException exception) {
        return switch (exception.getErrorCode()) {
            case AiServiceException.CODE_TIMEOUT -> "AI 服务响应超时，请稍后重试";
            case AiServiceException.CODE_INVALID_INPUT -> "症状描述格式有误，请重新输入";
            default -> "AI 服务暂时不可用，请联系工作人员";
        };
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        } finally {
            exchange.close();
        }
    }

    /**
     * 获取客户端 IP 地址。
     */
    private String getClientIp(HttpExchange exchange) {
        InetSocketAddress addr = exchange.getRemoteAddress();
        if (addr != null) {
            return addr.getAddress().getHostAddress() + ":" + addr.getPort();
        }
        return "unknown";
    }
}
