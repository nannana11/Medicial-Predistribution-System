# Release 使用说明

本目录包含医疗预分诊系统客户端 release 的说明文件。大型客户端 zip 和 `.app` 请通过 GitHub Releases 下载；本地打包时也会生成在 `release/` 目录下。客户端需要连接已启动的 `triage-server` 才能提交症状并获得分诊结果。

## 文件说明

```text
release/
├── README.md
├── RELEASE_NOTES.md
└── macos/
    ├── server-url.txt
    └── start-with-server-url.command
```

GitHub Releases 附件：

```text
MedicalTriageClient-1.0.0-macos-arm64.zip
MedicalTriageClient-1.0.0-windows-x64.zip
```

## 服务器地址配置

客户端 release 通过 `server-url.txt` 指定服务器地址。请将其中最后一行改为服务器电脑的局域网地址，例如：

```text
http://192.168.1.100:8080
```

地址必须包含协议和端口：

```text
http://服务器IP:8080
```

启动前建议先检查服务器健康接口：

```bash
curl http://192.168.1.100:8080/api/health
```

如果返回包含 `status: UP`，说明服务器可达。

## macOS 使用方法

适用平台：Apple Silicon Mac，即 M1/M2/M3/M4 等 arm64 设备。

1. 从 GitHub Releases 下载并解压 `MedicalTriageClient-1.0.0-macos-arm64.zip`。
2. 打开解压后的 `MedicalTriageClient-macos` 文件夹。
3. 修改 `server-url.txt`。
4. 双击 `start-with-server-url.command` 启动客户端。

也可以直接使用仓库中的：

```text
release/macos/MedicalTriageClient.app
release/macos/server-url.txt
release/macos/start-with-server-url.command
```

注意：如果直接双击 `MedicalTriageClient.app`，不会读取旁边的 `server-url.txt`。请通过 `start-with-server-url.command` 启动。

首次使用语音输入时，macOS 可能会请求麦克风权限，请选择允许。

## Windows 使用方法

适用平台：Windows x64。

1. 从 GitHub Releases 下载并解压 `MedicalTriageClient-1.0.0-windows-x64.zip`。
2. 打开解压后的 `MedicalTriageClient` 文件夹。
3. 修改 `server-url.txt`。
4. 双击 `start-with-server-url.bat` 启动客户端。

注意：不要只复制 `TriageClient.exe`，它依赖同目录下的 `jre`、`lib`、`app` 和 `models` 文件夹。也不要直接双击 `TriageClient.exe`，否则不会读取 `server-url.txt`。

## 常见问题

### 提示无法连接服务器

通常是以下原因：

- 服务器没有启动。
- IP 地址写错。
- 客户端和服务器不在同一个局域网。
- 服务器电脑防火墙拦截了 8080 端口。
- 没有通过启动脚本启动客户端。

### 提示 502、503 或服务器暂时无法处理

这说明客户端已经请求到了服务器，但服务器处理失败。请检查服务器控制台日志、DeepSeek API Key、网络代理和 `triage.ai.mock` 配置。

### 语音输入无法使用

请确认：

- 已允许应用访问麦克风。
- `models` 目录完整。
- release 文件夹没有只复制部分文件。

## 医疗安全提醒

本系统仅用于课程展示和预分诊流程演示，不能替代医生诊断。症状严重或快速加重时，应立即联系现场医护人员或前往急诊。
