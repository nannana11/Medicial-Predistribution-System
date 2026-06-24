package com.example.triageclient.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public final class ClientConfig {
    private static final String CONFIG_FILE = "/application.properties";
    private static final String BASE_URL_ENV = "TRIAGE_SERVER_BASE_URL";
    private static final String SPEECH_MODEL_DIR_ENV = "TRIAGE_SPEECH_MODEL_DIR";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String DEFAULT_SPEECH_MODEL_DIR =
            "models/sherpa-onnx-streaming-zipformer-zh-xlarge-int8-2025-06-30";

    private final String baseUrl;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final String speechModelDir;

    private ClientConfig(String baseUrl, Duration connectTimeout, Duration requestTimeout,
                         String speechModelDir) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.speechModelDir = normalizeSpeechModelDir(speechModelDir);
    }

    public static ClientConfig load() {
        Properties properties = new Properties();
        try (InputStream input = ClientConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取客户端配置文件。", exception);
        }

        String configuredUrl = System.getenv(BASE_URL_ENV);
        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = properties.getProperty("server.base-url", DEFAULT_BASE_URL);
        }
        String configuredSpeechModelDir = System.getenv(SPEECH_MODEL_DIR_ENV);
        if (configuredSpeechModelDir == null || configuredSpeechModelDir.isBlank()) {
            configuredSpeechModelDir = properties.getProperty("speech.model-dir", DEFAULT_SPEECH_MODEL_DIR);
        }

        int connectSeconds = readPositiveInt(properties, "server.connect-timeout-seconds", 5);
        int requestSeconds = readPositiveInt(properties, "server.request-timeout-seconds", 30);
        return new ClientConfig(
                configuredUrl,
                Duration.ofSeconds(connectSeconds),
                Duration.ofSeconds(requestSeconds),
                configuredSpeechModelDir);
    }

    private static int readPositiveInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? DEFAULT_BASE_URL : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeSpeechModelDir(String value) {
        return value == null || value.isBlank() ? DEFAULT_SPEECH_MODEL_DIR : value.trim();
    }

    public String triageEndpoint() {
        return baseUrl + "/api/triage/message";
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public String speechModelDir() {
        return speechModelDir;
    }
}
