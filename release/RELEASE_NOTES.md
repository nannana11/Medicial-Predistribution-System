# Release Notes

## v1.0.0 - 最终课程演示版

发布日期：2026-06-24

本版本是医疗预分诊系统的最终演示 release，包含可直接运行的客户端包、源码和本地语音识别资源。

## 发布内容

- macOS Apple Silicon 客户端 release 附件：
  - `MedicalTriageClient-1.0.0-macos-arm64.zip`
- Windows x64 客户端 release 附件：
  - `MedicalTriageClient-1.0.0-windows-x64.zip`
- 客户端源码：
  - `triage-client/`
- 服务端源码：
  - `triage-server/`
- 本地语音识别模型和 native 依赖：
  - 完整模型包含在 macOS / Windows release 包内
  - native 依赖位于 `triage-client/libs/`

## 主要功能

- JavaFX 中文图形界面
- 症状文本输入
- 本地离线中文语音识别输入
- HTTP/JSON 请求分诊服务器
- 推荐科室展示
- 紧急程度和急诊提示展示
- 网络失败、请求超时、服务器异常提示
- Mock 分诊模式
- DeepSeek 分诊模式
- 局域网服务器地址可配置

## 服务器地址配置

release 包新增了外部服务器地址文件：

```text
server-url.txt
```

用户只需要修改该文件中的地址，例如：

```text
http://192.168.1.100:8080
```

然后通过启动脚本启动客户端：

- macOS: `start-with-server-url.command`
- Windows: `start-with-server-url.bat`

注意：当前版本的客户端本体不会自动读取 `server-url.txt`。必须通过上述启动脚本启动，脚本会读取 `server-url.txt` 并设置 `TRIAGE_SERVER_BASE_URL` 环境变量。

## API 兼容性

客户端依赖以下服务器接口：

```http
GET /api/health
POST /api/triage/message
```

分诊接口请求：

```json
{
  "message": "我最近咳嗽、发烧"
}
```

分诊接口响应：

```json
{
  "success": true,
  "recommendedDepartment": "发热门诊",
  "urgencyLevel": "high",
  "needEmergency": true,
  "reply": "建议立即前往发热门诊就诊。"
}
```

## 构建说明

本版本 release 由以下组件打包：

- JavaFX 客户端 jar
- JavaFX 平台 native jar
- Jackson 运行依赖
- sherpa-onnx Java API
- sherpa-onnx macOS arm64 / Windows x64 native jar
- 中文语音识别模型
- 平台运行时

Windows release 使用 Launch4j 生成 `.exe` 启动器。macOS release 使用 `jpackage` 生成 `.app`。

## 已知限制

- 直接双击 `.app` 或 `.exe` 不会读取 `server-url.txt`，请使用启动脚本。
- macOS release 面向 Apple Silicon，不保证 Intel Mac 可用。
- Windows release 面向 x64，不保证 ARM Windows 可用。
- 当前服务器没有 HTTPS、认证和权限控制，不应直接暴露到公网。
- DeepSeek 模式需要配置 `DEEPSEEK_API_KEY`，不要将 API Key 提交到仓库。
- 本系统仅用于课程演示，不可作为真实诊断系统使用。

## 验证记录

已验证：

- macOS release 包含 `.app` 可执行文件。
- Windows release 包含 `TriageClient.exe`。
- 两个平台 release 均包含语音识别模型。
- 两个平台 release 均包含对应平台的 sherpa-onnx native 依赖。
- 两个平台 release 均包含服务器地址配置文件和启动脚本。
- 使用局域网服务器地址可成功调用 `/api/health` 和 `/api/triage/message`。
