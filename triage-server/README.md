# 医疗预分诊服务器

这是医疗预分诊系统的 HTTP/JSON 服务端，为 JavaFX 客户端提供健康检查和症状分诊接口。

> 本服务仅用于课程学习和预分诊流程演示，不能替代医生诊断。

## 接口

```text
GET  /api/health
POST /api/triage/message
```

默认端口：

```text
8080
```

## 源码运行

环境要求：

- JDK 17 或更高版本
- Maven 3.9 或更高版本

在 IntelliJ IDEA 中运行：

```text
src/main/java/com/triage/MainApplication.java
```

命令行运行：

```bash
mvn package
java -jar target/triage-server-1.0.0.jar
```

启动后检查：

```bash
curl http://localhost:8080/api/health
```

## 配置

配置文件：

```text
src/main/resources/application.properties
```

常用配置：

```properties
triage.server.port=8080
triage.ai.mock=false
triage.deepseek.api.url=https://api.deepseek.com/v1/chat/completions
triage.deepseek.model=deepseek-chat
triage.ai.timeout.ms=30000
```

如果使用 DeepSeek 模式，需要设置环境变量：

```bash
export DEEPSEEK_API_KEY=your-api-key
```

如果不想联网或没有 API Key，可以改为 Mock 模式：

```properties
triage.ai.mock=true
```

不要将真实 API Key 写入源码或提交到 GitHub。

## 请求示例

健康检查：

```bash
curl http://localhost:8080/api/health
```

提交分诊：

```bash
curl -H "Content-Type: application/json" \
  -d '{"message":"我现在发烧了"}' \
  http://localhost:8080/api/triage/message
```

响应示例：

```json
{
  "success": true,
  "recommendedDepartment": "发热门诊",
  "urgencyLevel": "high",
  "needEmergency": true,
  "reply": "建议立即前往发热门诊就诊，并做好个人防护。"
}
```

## 局域网联调

客户端连接远程服务器时，请确认：

1. 客户端和服务器在同一个局域网。
2. 服务器电脑已经运行本服务。
3. 服务器电脑防火墙允许 8080 端口。
4. 客户端 `server-url.txt` 或 `application.properties` 使用服务器电脑 IPv4 地址。

示例：

```text
http://192.168.1.100:8080
```

## 目录结构

```text
src/main/java/com/triage/
├── MainApplication.java
├── ai/
├── config/
├── protocol/
├── service/
└── transport/

src/main/resources/
├── application.properties
└── logback.xml
```
