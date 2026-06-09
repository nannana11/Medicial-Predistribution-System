package com.example.triageclient.service;

import com.example.triageclient.config.ClientConfig;
import com.example.triageclient.dto.TriageRequest;
import com.example.triageclient.dto.TriageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TriageApiService {
    private final ClientConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TriageApiService() {
        this(ClientConfig.load(), new ObjectMapper());
    }

    TriageApiService(ClientConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .build();
    }

    public CompletableFuture<TriageResponse> submitSymptoms(String message) {
        final String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(new TriageRequest(message));
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(
                    new TriageApiException("无法生成请求数据，请联系工作人员。", exception));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.triageEndpoint()))
                .timeout(config.requestTimeout())
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw new CompletionException(toFriendlyException(throwable));
                    }
                    return parseResponse(response);
                });
    }

    private TriageResponse parseResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new TriageApiException("服务器暂时无法处理请求（状态码 "
                    + response.statusCode() + "），请稍后重试。");
        }

        try {
            TriageResponse result = objectMapper.readValue(response.body(), TriageResponse.class);
            if (!result.isSuccess()) {
                String message = hasText(result.getReply())
                        ? result.getReply()
                        : "系统暂时无法生成预分诊建议，请联系现场工作人员。";
                throw new TriageApiException(message);
            }
            if (!hasText(result.getReply())) {
                throw new TriageApiException("服务器返回的数据不完整，请联系工作人员。");
            }
            return result;
        } catch (JsonProcessingException exception) {
            throw new TriageApiException("服务器返回了无法识别的数据，请联系工作人员。", exception);
        }
    }

    private TriageApiException toFriendlyException(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof HttpTimeoutException) {
            return new TriageApiException("请求超时，请检查网络后重试。", cause);
        }
        if (cause instanceof ConnectException) {
            return new TriageApiException("当前无法连接服务器，请联系工作人员。", cause);
        }
        if (cause instanceof IOException) {
            return new TriageApiException("网络通信失败，请检查网络后重试。", cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new TriageApiException("请求已中断，请重新提交。", cause);
        }
        if (cause instanceof TriageApiException apiException) {
            return apiException;
        }
        return new TriageApiException("系统发生未知错误，请联系工作人员。", cause);
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
