package com.triage.ai;

/**
 * AI 客户端接口。
 * <p>
 * 定义与云端大模型交互的标准方法，所有 AI 客户端实现必须遵循此接口。
 * 当前支持 {@link MockAiClient}（模拟）和 {@link DeepSeekClient}（真实预留）。
 * </p>
 */
public interface AiClient {

    /**
     * 将患者症状描述发送给 AI 模型进行分析，返回分诊结果。
     * <p>
     * 输入为原始症状文本，输出为包含分诊科室、严重程度、建议等信息的 JSON 字符串。
     * 实现类应确保：
     * <ul>
     *   <li>对空症状返回友好的错误信息</li>
     *   <li>处理网络超时、API 异常等情况</li>
     *   <li>进行必要的参数校验</li>
     * </ul>
     * </p>
     *
     * @param symptoms 患者症状描述文本，不应为 null 或空字符串
     * @return 分诊结果 JSON 字符串。格式示例：
     *         {@code {"department": "内科", "severity": "普通", "advice": "建议挂号内科门诊", "confidence": 0.85}}
     * @throws IllegalArgumentException 如果 symptoms 为 null 或空
     * @throws AiServiceException       如果 AI 调用过程中发生异常
     */
    String analyze(String symptoms) throws IllegalArgumentException, AiServiceException;
}
