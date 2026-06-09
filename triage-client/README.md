# 医疗预分诊系统客户端（第一阶段）

这是“人工智能辅助医院门诊预分诊系统”的 JavaFX 客户端。客户端负责接收患者的症状描述，通过 HTTP/JSON 请求 `triage-server`，并展示推荐科室、紧急程度、急诊提示和系统回复。

> 本项目只用于课程学习和门诊预分诊演示，不能替代医生诊断，也不提供具体用药建议。

## 已完成功能

- JavaFX 中文图形界面
- 症状输入与空内容校验
- 最长 1000 字符限制
- 异步请求后端，避免界面卡死
- 解析结构化 JSON 分诊结果
- 展示推荐科室、紧急程度、急诊提示和回复
- 连接失败、请求超时、服务器异常、数据异常提示
- 服务器地址和超时时间配置

## 环境要求

- JDK 21 或更高版本
- Maven 3.9 或更高版本
- 已启动的分诊服务器

## 后端接口约定

客户端请求：

```http
POST /api/triage/message
Content-Type: application/json
```

```json
{
  "message": "我最近咳嗽、发烧、胸闷"
}
```

后端应返回：

```json
{
  "success": true,
  "recommendedDepartment": "呼吸内科",
  "urgencyLevel": "medium",
  "needEmergency": false,
  "reply": "根据您的描述，建议优先咨询呼吸内科。"
}
```

## 配置服务器地址

打开 `src/main/resources/application.properties`：

```properties
server.base-url=http://10.135.72.117:8080
server.connect-timeout-seconds=5
server.request-timeout-seconds=30
```

当前地址用于连接搭档的服务器。如果搭档电脑重新联网后 IP 地址发生变化，
需要将它替换为搭档最新的 WLAN IPv4，例如：

```properties
server.base-url=http://192.168.1.100:8080
```

也可以通过环境变量 `TRIAGE_SERVER_BASE_URL` 临时覆盖该地址。

## IntelliJ IDEA 运行方法

1. 使用 IntelliJ IDEA 打开本文件夹。
2. 等待 IDEA 识别 `pom.xml` 并下载 Maven 依赖。
3. 确认项目 SDK 为 JDK 21 或更高版本。
4. 先启动小组成员开发的 Spring Boot 后端。
5. 推荐在 Maven 工具窗口运行 `Plugins -> javafx -> javafx:run`，该配置会为
   JDK 25 添加 JavaFX 所需的本地访问参数。

也可以打开 `Launcher.java` 并运行 `main`。客户端已加入 `module-info.java`，
不会再以不受支持的 unnamed module 方式加载 JavaFX。JDK 25 仍可能输出来自
JavaFX 21 内部实现的兼容性警告，它不代表运行失败；退出代码 `0` 表示正常结束。

也可以在已安装 Maven 的终端中运行：

```powershell
mvn clean test
mvn javafx:run
```

## 第一阶段联调检查

正常联调时请先运行 `triage-server` 中的 `MainApplication.java`。

远程联调时，应由搭档在他的电脑上运行 `MainApplication.java`，你的电脑只需
运行客户端。启动客户端前可在 PowerShell 中检查：

```powershell
Test-NetConnection 10.135.72.117 -Port 8080
Invoke-RestMethod http://10.135.72.117:8080/api/health
```

只有看到 `TcpTestSucceeded : True` 且健康接口返回 `status: UP` 后，才表示
远程服务器已经可以使用。

如果需要单独测试客户端，也可以运行
`src/test/java/com/example/triageclient/MockTriageServer.java`，再运行
`Launcher.java`。测试服务器会监听 `http://localhost:8080` 并返回固定的分诊结果。

1. 输入为空时，客户端应提示填写症状。
2. 后端正常时，应显示结构化分诊结果。
3. 后端关闭时，应提示“当前无法连接服务器”。
4. 后端响应过慢时，应提示请求超时。
5. 返回 `success: false` 时，应显示后端提供的友好错误信息。

## 项目结构

```text
src/main/java/com/example/triageclient/
├── TriageClientApplication.java
├── config/ClientConfig.java
├── controller/MainViewController.java
├── dto/TriageRequest.java
├── dto/TriageResponse.java
├── service/TriageApiService.java
├── service/TriageApiException.java
└── util/AlertUtil.java

src/main/resources/
├── application.properties
├── css/style.css
└── fxml/main-view.fxml
```
