package com.triage.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 协议解析器。
 * <p>
 * 负责解析分机发送的 JSON 请求，提取 symptoms 字段；
 * 以及封装分诊结果为 JSON 响应字符串，回传给分机。
 * </p>
 *
 * <h3>请求格式（分机→主机）：</h3>
 * <pre>
 * {
 *   "client_id": "ext-001",
 *   "symptoms": "头痛、发热、咳嗽持续两天",
 *   "timestamp": "2026-06-05T10:30:00"
 * }
 * </pre>
 *
 * <h3>响应格式（主机→分机）：</h3>
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {
 *     "department_id": "respiratory",
 *     "department": "呼吸内科",
 *     "severity": "紧急",
 *     "advice": "建议立即前往呼吸内科就诊",
 *     "confidence": 0.85
 *   },
 *   "request_id": "req-001"
 * }
 * </pre>
 *
 * <h3>错误响应格式：</h3>
 * <pre>
 * {
 *   "code": 400,
 *   "message": "症状描述不能为空",
 *   "data": null,
 *   "request_id": "req-001"
 * }
 * </pre>
 */
public class ProtocolParser {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolParser.class);

    private final ObjectMapper objectMapper;

    public ProtocolParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 解析分机发送的 JSON 请求，提取症状描述文本。
     *
     * @param jsonRequest 分机发送的 JSON 字符串
     * @return 解析后的请求对象，包含 clientId 和 symptoms
     * @throws ProtocolParseException 如果 JSON 格式错误或缺少必要字段
     */
    public TriageRequest parseRequest(String jsonRequest) throws ProtocolParseException {
        if (jsonRequest == null || jsonRequest.trim().isEmpty()) {
            throw new ProtocolParseException("请求内容为空", 400, "EMPTY_REQUEST");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(jsonRequest, Map.class);

            String clientId = getStringValue(map, "client_id", "");
            String symptoms = getStringValue(map, "symptoms", "");
            String requestId = getStringValue(map, "request_id", "");

            if (symptoms.isEmpty()) {
                logger.warn("请求缺少 symptoms 字段或为空, clientId={}", clientId);
                throw new ProtocolParseException("症状描述(symptoms)不能为空", 400, "EMPTY_SYMPTOMS");
            }

            if (clientId.isEmpty()) {
                logger.warn("请求缺少 client_id 字段");
                throw new ProtocolParseException("客户端ID(client_id)不能为空", 400, "MISSING_CLIENT_ID");
            }

            TriageRequest request = new TriageRequest(clientId, symptoms, requestId);
            logger.debug("解析请求成功: clientId={}, requestId={}", clientId, requestId);
            return request;

        } catch (ProtocolParseException e) {
            throw e;
        } catch (Exception e) {
            logger.error("解析 JSON 请求失败: {}", jsonRequest, e);
            throw new ProtocolParseException("JSON 格式错误: " + e.getMessage(), 400, "INVALID_JSON");
        }
    }

    /**
     * 封装成功分诊结果为 JSON 响应字符串。
     *
     * @param requestId    请求ID
     * @param triageResult AI 返回的分诊结果 JSON 字符串
     * @return 完整响应 JSON 字符串
     */
    public String buildSuccessResponse(String requestId, String triageResult) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 200);
            response.put("message", "success");

            // 如果 triageResult 已经是 JSON，将其解析为 Map 后放入 data 字段
            Object data;
            if (triageResult != null && triageResult.trim().startsWith("{")) {
                data = objectMapper.readValue(triageResult, Map.class);
            } else {
                data = triageResult;
            }
            response.put("data", data);
            response.put("request_id", requestId != null ? requestId : "");

            String json = objectMapper.writeValueAsString(response);
            logger.debug("构建成功响应: requestId={}", requestId);
            return json;
        } catch (Exception e) {
            logger.error("构建成功响应失败", e);
            return "{\"code\":500,\"message\":\"服务器内部错误\"}";
        }
    }

    /**
     * 封装错误信息为 JSON 响应字符串。
     *
     * @param requestId 请求ID
     * @param code      错误码
     * @param message   错误描述
     * @return 错误响应 JSON 字符串
     */
    public String buildErrorResponse(String requestId, int code, String message) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", code);
            response.put("message", message);
            response.put("data", null);
            response.put("request_id", requestId != null ? requestId : "");

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("构建错误响应失败", e);
            return "{\"code\":500,\"message\":\"服务器内部错误\"}";
        }
    }

    /**
     * 从 Map 中安全获取字符串值。
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    // ==================== 内部数据类 ====================

    /**
     * 分诊请求数据类。
     */
    public static class TriageRequest {
        private final String clientId;
        private final String symptoms;
        private final String requestId;

        public TriageRequest(String clientId, String symptoms, String requestId) {
            this.clientId = clientId;
            this.symptoms = symptoms;
            this.requestId = requestId;
        }

        public String getClientId() { return clientId; }
        public String getSymptoms() { return symptoms; }
        public String getRequestId() { return requestId; }
    }
}
