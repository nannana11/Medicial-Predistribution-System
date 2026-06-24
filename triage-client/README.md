# 医疗预分诊系统客户端

这是医疗预分诊系统的 JavaFX 客户端。客户端负责接收患者症状描述，支持文本输入和本地离线中文语音识别，并通过 HTTP/JSON 请求 `triage-server` 获取推荐科室、紧急程度、急诊提示和系统回复。

> 本客户端仅用于课程学习和预分诊流程演示，不能替代医生诊断，也不提供具体用药建议。

## 功能

- JavaFX 中文图形界面
- 本地离线中文语音输入，基于 sherpa-onnx
- 症状输入校验和 1000 字符限制
- 异步请求服务器，避免界面卡顿
- 结构化展示推荐科室、紧急程度和回复
- 连接失败、请求超时和服务器异常提示
- 支持通过环境变量覆盖服务器地址

## 源码运行

环境要求：

- JDK 21 或更高版本
- Maven 3.9 或更高版本
- 已启动的 `triage-server`

运行：

```bash
mvn javafx:run
```

或在 IntelliJ IDEA 中运行：

```text
src/main/java/com/example/triageclient/Launcher.java
```

## 服务器地址

源码运行时，默认配置位于：

```text
src/main/resources/application.properties
```

示例：

```properties
server.base-url=http://10.133.33.211:8080
server.connect-timeout-seconds=5
server.request-timeout-seconds=30
```

局域网联调时，请将地址改为服务器电脑的 IPv4 地址：

```properties
server.base-url=http://192.168.1.100:8080
```

也可以通过环境变量覆盖：

```bash
export TRIAGE_SERVER_BASE_URL=http://192.168.1.100:8080
```

release 包中请修改 `server-url.txt`，并通过启动脚本启动：

- macOS: `start-with-server-url.command`
- Windows: `start-with-server-url.bat`

## 语音识别

源码运行语音识别时需要本地模型。模型文件体积较大，不提交到普通 Git 仓库；最终 release 包中已经包含完整模型。

模型目录：

```text
models/sherpa-onnx-streaming-zipformer-zh-xlarge-int8-2025-06-30
```

可通过环境变量覆盖：

```bash
export TRIAGE_SPEECH_MODEL_DIR=/path/to/model
```

首次点击语音输入时会加载模型，可能需要等待几秒。macOS 首次录音需要允许应用访问麦克风。

## 后端接口

客户端请求：

```http
POST /api/triage/message
Content-Type: application/json
```

请求体：

```json
{
  "message": "我最近咳嗽、发烧"
}
```

期望响应：

```json
{
  "success": true,
  "recommendedDepartment": "发热门诊",
  "urgencyLevel": "high",
  "needEmergency": true,
  "reply": "建议立即前往发热门诊就诊。"
}
```

## 项目结构

```text
src/main/java/com/example/triageclient/
├── Launcher.java
├── TriageClientApplication.java
├── config/
├── controller/
├── dto/
├── service/
└── util/

src/main/resources/
├── application.properties
├── css/style.css
├── fxml/main-view.fxml
└── images/background.jpg
```
