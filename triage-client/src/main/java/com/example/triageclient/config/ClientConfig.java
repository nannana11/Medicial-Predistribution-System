package com.example.triageclient.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public final class ClientConfig {
    private static final String CONFIG_FILE = "/application.properties";
    private static final String BASE_URL_ENV = "TRIAGE_SERVER_BASE_URL";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final String baseUrl;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    private ClientConfig(String baseUrl, Duration connectTimeout, Duration requestTimeout) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
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

        int connectSeconds = readPositiveInt(properties, "server.connect-timeout-seconds", 5);
        int requestSeconds = readPositiveInt(properties, "server.request-timeout-seconds", 30);
        return new ClientConfig(
                configuredUrl,
                Duration.ofSeconds(connectSeconds),
                Duration.ofSeconds(requestSeconds));
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

    public String triageEndpoint() {
        return baseUrl + "/api/triage/message";
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }
}
