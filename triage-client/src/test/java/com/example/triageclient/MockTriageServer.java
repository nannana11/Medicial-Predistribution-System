package com.example.triageclient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 仅用于第一阶段客户端联调，不属于正式服务器代码。
 */
public final class MockTriageServer {
    private MockTriageServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/api/triage/message", MockTriageServer::handleTriage);
        server.start();

        System.out.println("本地测试服务器已启动：http://localhost:8080");
        System.out.println("请保持此程序运行，然后启动 Launcher。");
    }

    private static void handleTriage(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, """
                    {"success":false,"reply":"测试服务器只接受 POST 请求。"}
                    """);
            return;
        }

        // 读取请求体，确保客户端已完整发送数据。
        exchange.getRequestBody().readAllBytes();
        sendJson(exchange, 200, """
                {
                  "success": true,
                  "recommendedDepartment": "呼吸内科",
                  "urgencyLevel": "medium",
                  "needEmergency": false,
                  "reply": "这是本地测试服务器返回的结果。客户端通信正常；正式联调时请启动真实后端。"
                }
                """);
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String body)
            throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
