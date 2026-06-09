# 医疗预分诊服务器

服务器对 JavaFX 客户端提供以下接口：

```text
POST /api/triage/message
GET  /api/health
```

默认监听 `8080` 端口并使用 `MockAiClient`，因此第一阶段联调不需要
DeepSeek API Key。

启动类：

```text
com.triage.MainApplication
```

配置文件：

```text
src/main/resources/application.properties
```

真实 AI 模式需要将 `triage.ai.mock=false`，并设置环境变量：

```text
DEEPSEEK_API_KEY=你的密钥
```

原有 Netty TCP 实现保留在 `transport/NettyServer.java`，目前第一阶段入口
使用与客户端规划一致的 HTTP/JSON 服务 `HttpTriageServer.java`。
