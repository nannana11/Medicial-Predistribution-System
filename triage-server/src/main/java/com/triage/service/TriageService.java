package com.triage.service;

import com.triage.ai.AiClient;
import com.triage.ai.AiServiceException;
import com.triage.ai.DeepSeekClient;
import com.triage.ai.MockAiClient;
import com.triage.protocol.ProtocolParser;
import com.triage.protocol.ProtocolParser.TriageRequest;
import com.triage.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分诊业务服务类。
 * <p>
 * 核心业务逻辑编排层，负责：
 * <ol>
 *   <li>调用 {@link ProtocolParser} 解析分机请求</li>
 *   <li>调用 {@link AiClient} 进行 AI 分诊分析</li>
 *   <li>调用 {@link ProtocolParser} 封装响应结果</li>
 * </ol>
 * </p>
 *
 * <h3>切换 AI 客户端：</h3>
 * <p>
 * 当前使用 {@link MockAiClient} 返回模拟结果。
 * 如需切换为真实 DeepSeek API，请将构造函数中 MockAiClient 替换为 DeepSeekClient：
 * <pre>{@code
 * // 真实模式
 * private final AiClient aiClient = new DeepSeekClient();
 * }</pre>
 * </p>
 */
public class TriageService {

    private static final Logger logger = LoggerFactory.getLogger(TriageService.class);

    private final ProtocolParser protocolParser;
    private final AiClient aiClient;

    public TriageService() {
        this.protocolParser = new ProtocolParser();

        // ============================================================
        // 【切换开关】如需使用真实 DeepSeek API，请编辑下一行的注释
        // 【切换开关】如需使用Mock测试模式，请编辑下一行的注释
        // ============================================================
        //this.aiClient = new MockAiClient();
        //this.aiClient = new DeepSeekClient();
        Config config = Config.getInstance();
        if(config.isMockMode()){
            this.aiClient = new MockAiClient();
            logger.info("当前模式：Mock");
        }else{
            this.aiClient = new DeepSeekClient();
            logger.info("当前模式：Practice");
        }
    }

    /**
     * 处理一条来自分机的完整分诊请求。
     * <p>
     * 流程：解析请求 → AI 分析 → 封装响应。
     * 所有异常均被捕获并转换为友好的错误响应字符串。
     * </p>
     *
     * @param rawMessage 分机发送的原始 JSON 字符串
     * @return 响应 JSON 字符串（包含分诊结果或错误信息）
     */
    public String processTriage(String rawMessage) {
        // 前置检查：空消息
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            logger.warn("收到空消息");
            return protocolParser.buildErrorResponse("", 400, "消息内容不能为空");
        }

        try {
            // 步骤1：解析 JSON 请求，提取症状
            TriageRequest request = protocolParser.parseRequest(rawMessage);
            String requestId = request.getRequestId();
            String symptoms = request.getSymptoms();
            String clientId = request.getClientId();

            logger.info("开始处理分诊请求: clientId={}, requestId={}, symptoms={}",
                    clientId, requestId, symptoms);

            // 步骤2：调用 AI 进行分诊分析
            String aiResult = aiClient.analyze(symptoms);

            logger.info("AI 分诊完成: clientId={}, requestId={}, result={}",
                    clientId, requestId, aiResult);

            // 步骤3：封装成功响应
            return protocolParser.buildSuccessResponse(requestId, aiResult);

        } catch (com.triage.protocol.ProtocolParseException e) {
            // 协议解析异常（JSON 格式错误、缺少字段等）
            logger.warn("协议解析失败: {}", e.getMessage());
            return protocolParser.buildErrorResponse("", e.getHttpCode(), e.getMessage());

        } catch (AiServiceException e) {
            // AI 服务异常（超时、API 错误等）
            logger.error("AI 服务异常: errorCode={}, message={}", e.getErrorCode(), e.getMessage(), e);
            String userMessage = switch (e.getErrorCode()) {
                case "TIMEOUT" -> "AI 服务响应超时，请稍后重试";
                case "API_ERROR" -> "AI 服务暂时不可用，请稍后重试";
                case "INVALID_INPUT" -> "症状描述格式有误，请重新输入";
                default -> "分诊服务处理异常，请联系管理员";
            };
            return protocolParser.buildErrorResponse("", 500, userMessage);

        } catch (Exception e) {
            // 其他未知异常
            logger.error("处理分诊请求时发生未知异常", e);
            return protocolParser.buildErrorResponse("", 500, "服务器内部错误，请稍后重试");
        }
    }

    /**
     * 供 HTTP 接口直接调用已有 AI 分诊能力。
     *
     * @param symptoms 用户症状描述
     * @return AI 返回的结构化 JSON
     */
    public String analyzeSymptoms(String symptoms) {
        if (symptoms == null || symptoms.isBlank()) {
            throw new IllegalArgumentException("症状描述不能为空");
        }
        return aiClient.analyze(symptoms.trim());
    }
}
