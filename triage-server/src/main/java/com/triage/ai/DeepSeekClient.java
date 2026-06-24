package com.triage.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triage.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API 客户端（预留真实接口）。
 * <p>
 * 通过 HTTP 调用 DeepSeek 云 API 进行分诊分析。
 * 使用 JDK 11+ 内置的 {@link java.net.http.HttpClient}，无需额外 HTTP 依赖。
 * </p>
 *
 * <h3>使用前配置：</h3>
 * <ol>
 *   <li>在 application.properties 中填写 {@code triage.deepseek.api.key}</li>
 *   <li>或通过启动参数传入：{@code -Dtriage.deepseek.api.key=sk-your-key-here}</li>
 *   <li>可选：自定义 API URL 和模型名称</li>
 * </ol>
 *
 * <h3>切换为真实模式：</h3>
 * 在 {@link com.triage.service.TriageService} 中将 MockAiClient 替换为此类。
 */
public class DeepSeekClient implements AiClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekClient.class);

    private final Config config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * 系统提示词（System Prompt），指导 AI 扮演分诊医生的角色。
     * <p>
     * 【预留位置】可根据实际需求修改此提示词以优化分诊效果。
     * </p>
     */
    // TODO: 根据实际需求调整 system prompt 内容
    private static final String SYSTEM_PROMPT = """
            你是一名资深分诊医生。请根据患者的症状描述，判断应挂号的科室和紧急程度。
            假如患者的描述与症状无关，返回：“请不要输入无关内容”；
            如果患者输入的是症状，
            请严格按照以下 JSON 格式返回结果（不要包含多余文字）：
            {
              "department_id": "科室ID（英文小写）",
              "department": "科室名称（中文）",
              "severity": "紧急程度（紧急/普通/非紧急）",
              "advice": "就诊建议",
              "confidence": 置信度（0~1之间的小数）
            }
            
            下面是医院的结构：
            一、 非手术科室
            
            · 内科系：内科（内科学部）、呼吸与危重症医学科、消化内科、肾脏内科、心血管内科、血液内科、内分泌代谢科、风湿免疫科。
            · 综合/老年/心理/重症：变态反应科、老年医学中心/干部医疗科、神经内科、精神卫生中心、康复医学科/康复医学中心、感染性疾病中心、全科医学中心、急诊科、重症医学科。
            · 中西医结合：中西医结合科（下设 中西医结合内科、中西医结合外科、中西医结合科）。
            · 肿瘤中心：下设 肿瘤内科、放疗科、生物治疗科。
              · 肿瘤中心下属专科门诊：头颈肿瘤病房、胸部肿瘤病房、腹部肿瘤病房。
            · 全科医学中心：下设 全科医学中心办公室、全科（特需）病房、健康管理中心、日间服务中心。
            二、 手术科室
            · 外科系：外科（外科学部）、整形外科/烧伤科、骨科、泌尿外科、神经外科、胸外科、心脏大血管外科、小儿外科、器官移植中心、普外科、创伤医学中心、眼科、耳鼻咽喉-头颈外科、麻醉手术中心、疼痛科、介入诊疗中心。
            · 外科病房细分（从属各外科）：甲状腺外科病房、胃外科病房、肝脏外科病房、胆道外科病房、胰腺外科病房、血管外科病房。
            三、 专病中心
            · 妇科中心（下属： 卵巢癌中心、宫颈癌中心、结直肠癌中心、乳腺癌中心、胰腺炎中心）
            四、 医技科室
            · 影像与核医学：放射科、核医学科、超声医学科、放射物理技术中心。
            · 检验与病理：病理科、实验医学科（检验科）、神经生物检测中心、输血科。
            · 药学与营养：临床药学室（药剂科）、临床营养科。
            · 其他：呼吸治疗科、精准医学中心。
            """;

    public DeepSeekClient() {
        this.config = Config.getInstance();
        this.objectMapper = new ObjectMapper();

        // 创建 HTTP 客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String analyze(String symptoms) throws IllegalArgumentException, AiServiceException {
        // 参数校验
        if (symptoms == null || symptoms.trim().isEmpty()) {
            logger.warn("收到空症状，拒绝分析");
            throw new AiServiceException("症状描述不能为空", AiServiceException.CODE_INVALID_INPUT);
        }

        String apiKey = config.getDeepSeekApiKey();

        // 检查 API Key 是否已配置
        // TODO: 集成真实 API 时，请确保 application.properties 中已填写 API Key
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("API Key 未配置，请在 环境变量 中设置名为 API_KEY 的环境变量并指定你的deepseek API");
            throw new AiServiceException(
                    "API Key 未配置，请在 环境变量 中设置名为 API_KEY 的环境变量并指定你的deepseek API",
                    AiServiceException.CODE_API_ERROR);
        }

        logger.info("开始调用 DeepSeek API 分析症状: {}", symptoms);

        try {
            // 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getDeepSeekModel());

            // 构建消息列表
            Map<String, String> systemMessage = new LinkedHashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);

            Map<String, String> userMessage = new LinkedHashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "患者症状：" + symptoms);

            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("temperature", 0.3);   // 低温度确保输出稳定
            requestBody.put("max_tokens", 512);     // 限制输出长度

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 构建 HTTP 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDeepSeekApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(config.getAiTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // 检查响应状态
            if (response.statusCode() != 200) {
                logger.error("DeepSeek API 返回异常状态码: {}, body: {}",
                        response.statusCode(), response.body());
                throw new AiServiceException(
                        String.format("API 调用失败，状态码: %d", response.statusCode()),
                        AiServiceException.CODE_API_ERROR);
            }

            String responseBody = response.body();
            logger.info("DeepSeek API 响应成功: {}", responseBody);

            // 解析 API 响应，提取 AI 生成的文本内容
            // DeepSeek 兼容 OpenAI API 格式，响应结构为：
            // { "choices": [ { "message": { "content": "...", "role": "assistant" } } ] }
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new AiServiceException("API 返回的 choices 为空",
                        AiServiceException.CODE_PARSE_ERROR);
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String content = (String) message.get("content");

            if (content == null || content.trim().isEmpty()) {
                throw new AiServiceException("AI 返回内容为空",
                        AiServiceException.CODE_PARSE_ERROR);
            }

            logger.info("DeepSeek 分诊完成: symptoms={}, result={}", symptoms, content);
            return content.trim();

        } catch (AiServiceException e) {
            // 直接抛出自定义异常
            throw e;
        } catch (java.net.http.HttpConnectTimeoutException e) {
            logger.error("DeepSeek API 连接超时", e);
            throw new AiServiceException("AI 服务连接超时，请稍后重试",
                    AiServiceException.CODE_TIMEOUT, e);
        } catch (java.net.http.HttpTimeoutException e) {
            logger.error("DeepSeek API 请求超时", e);
            throw new AiServiceException("AI 服务请求超时，请稍后重试",
                    AiServiceException.CODE_TIMEOUT, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("DeepSeek API 请求被中断", e);
            throw new AiServiceException("AI 服务请求被中断",
                    AiServiceException.CODE_API_ERROR, e);
        } catch (Exception e) {
            logger.error("调用 DeepSeek API 时发生未知错误", e);
            throw new AiServiceException("AI 服务调用失败: " + e.getMessage(),
                    AiServiceException.CODE_API_ERROR, e);
        }
    }
}
