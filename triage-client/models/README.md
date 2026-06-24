# 语音识别模型目录

本目录用于放置 sherpa-onnx 本地离线中文语音识别模型。

模型文件体积较大，单个 `encoder.int8.onnx` 超过 GitHub 普通仓库文件大小限制，因此不会提交到源码仓库。

源码运行语音识别时，请将模型目录放在：

```text
triage-client/models/sherpa-onnx-streaming-zipformer-zh-xlarge-int8-2025-06-30/
```

最终 release 包中已经包含该模型，普通用户直接下载 release 即可使用语音输入。
