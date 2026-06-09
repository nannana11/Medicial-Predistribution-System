package com.triage.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟 AI 客户端。
 * <p>
 * 不真实调用云端 API，而是使用可解释的演示规则返回分诊结果。
 * 高危规则优先于普通科室规则；无法识别时要求用户补充症状。
 * </p>
 *
 * <h3>模拟规则说明：</h3>
 * <ul>
 *   <li>严重失血、呼吸困难、意识不清等 → 急诊科 / 紧急</li>
 *   <li>普通咳嗽、发热 → 呼吸内科 / 普通</li>
 *   <li>无法识别具体症状 → 暂无法判断 / 待补充</li>
 * </ul>
 */
public class MockAiClient implements AiClient {

    private static final Logger logger = LoggerFactory.getLogger(MockAiClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String analyze(String symptoms) throws IllegalArgumentException {
        // 参数校验
        if (symptoms == null || symptoms.trim().isEmpty()) {
            logger.warn("收到空症状，拒绝分析");
            throw new AiServiceException("症状描述不能为空", AiServiceException.CODE_INVALID_INPUT);
        }

        logger.debug("MockAiClient 开始分析症状: {}", symptoms);

        // 模拟 AI 处理延迟（100~300ms）
        try {
            Thread.sleep((long) (100 + Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        TriageRuleResult triageResult = evaluate(symptoms.toLowerCase());

        // 组装返回结果（保持字段顺序一致）
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("department_id", triageResult.departmentId());
            response.put("department", triageResult.department());
            response.put("severity", triageResult.severity());
            response.put("advice", triageResult.advice());
            response.put("confidence", triageResult.confidence());
            response.put("source", "mock-rules");

            String jsonResult = objectMapper.writeValueAsString(response);
            logger.info("MockAiClient 分诊完成: symptoms={}, result={}", symptoms, jsonResult);
            return jsonResult;
        } catch (Exception e) {
            logger.error("MockAiClient 序列化结果失败", e);
            throw new AiServiceException("AI 分析结果序列化失败", AiServiceException.CODE_PARSE_ERROR, e);
        }
    }

    private TriageRuleResult evaluate(String text) {
        // 高危规则必须最先判断，避免被普通科室关键词提前截获。
        if (containsAny(text, "大出血", "严重出血", "失血", "流血不止", "血止不住",
                "大量流血", "喷血", "呕血", "咳血")) {
            return emergency("severe-bleeding", "急诊科",
                    "存在严重出血或持续失血风险，请勿自行走动，立即呼叫现场医护人员，由工作人员启动院内急救流程并转送急诊科。");
        }
        if (containsAny(text, "呼吸困难", "喘不上气", "无法呼吸", "不能呼吸", "窒息",
                "嘴唇发紫", "口唇发紫")) {
            return emergency("breathing-emergency", "急诊科",
                    "存在呼吸困难等高危表现，请勿自行走动，立即呼叫现场医护人员，由工作人员启动院内急救流程并转送急诊科。");
        }
        if (containsAny(text, "意识不清", "失去意识", "昏迷", "昏厥", "叫不醒",
                "抽搐", "高热惊厥")) {
            return emergency("neurological-emergency", "急诊科",
                    "存在意识异常或抽搐等高危表现，请立即呼叫现场医护人员，由工作人员启动院内急救流程并转送急诊科。");
        }
        if (containsAny(text, "自杀", "轻生", "自残", "不想活")) {
            return emergency("mental-health-emergency", "急诊科",
                    "存在自伤风险，请不要独处，立即呼叫现场医护人员，并由工作人员全程陪同处置。");
        }
        if (containsAny(text, "胸痛", "胸口疼", "心口疼")
                && containsAny(text, "大汗", "冒汗", "冷汗", "呼吸困难", "喘不上气",
                "恶心", "晕厥")) {
            return emergency("chest-pain-emergency", "急诊科",
                    "胸痛伴随其他高危表现，请勿自行走动，立即呼叫现场医护人员，由工作人员转送急诊科。");
        }
        if (containsAny(text, "突发肢体无力", "一侧无力", "半身不遂", "口角歪斜",
                "说话不清", "言语不清")) {
            return emergency("stroke-warning", "急诊科",
                    "存在卒中警示表现，请勿自行等待或走动，立即呼叫现场医护人员，由工作人员转送急诊科。");
        }

        // 普通科室规则不把单一常见症状直接判定为急诊。
        if (containsAny(text, "咳嗽", "发热", "发烧", "咽痛", "鼻塞", "流鼻涕",
                "打喷嚏", "咳痰")) {
            return normal("respiratory", "呼吸内科",
                    "建议优先咨询呼吸内科。请补充症状持续时间、最高体温，以及是否伴有呼吸困难或胸痛。");
        }
        if (containsAny(text, "头痛", "头晕", "眩晕", "手麻", "脚麻")) {
            return normal("neurology", "神经内科",
                    "建议优先咨询神经内科。如症状突然出现或伴有肢体无力、言语不清，请立即前往急诊。");
        }
        if (containsAny(text, "腹痛", "肚子疼", "胃痛", "腹泻", "拉肚子", "恶心",
                "呕吐", "反酸")) {
            return normal("gastroenterology", "消化内科",
                    "建议优先咨询消化内科。如腹痛剧烈、持续加重或伴大量出血，请立即前往急诊。");
        }
        if (containsAny(text, "胸闷", "心悸", "心慌", "胸部不适", "胸口不舒服")) {
            return normal("cardiology", "心血管内科",
                    "建议优先咨询心血管内科。如出现持续胸痛、呼吸困难、冷汗或晕厥，请立即前往急诊。");
        }
        if (containsAny(text, "骨折", "扭伤", "摔伤", "外伤", "关节痛", "腰痛",
                "腿疼", "胳膊疼")) {
            return normal("orthopedics", "骨科",
                    "建议优先咨询骨科。如伤口大量出血、肢体明显变形或无法活动，请及时前往急诊。");
        }
        if (containsAny(text, "皮肤", "皮疹", "瘙痒", "起疹子", "红疹", "脱皮")) {
            return normal("dermatology", "皮肤科",
                    "建议优先咨询皮肤科。如伴呼吸困难、面唇肿胀或症状迅速扩散，请立即前往急诊。");
        }
        if (containsAny(text, "眼痛", "眼红", "视力下降", "看不清", "眼睛疼")) {
            return normal("ophthalmology", "眼科",
                    "建议优先咨询眼科。如视力突然明显下降或眼部严重外伤，请尽快急诊处理。");
        }
        if (containsAny(text, "耳痛", "耳鸣", "听不清", "鼻出血", "喉咙痛", "吞咽困难")) {
            return normal("ent", "耳鼻喉科",
                    "建议优先咨询耳鼻喉科。如无法吞咽、呼吸受阻或鼻出血无法止住，请立即前往急诊。");
        }

        return new TriageRuleResult(
                "needs-more-information",
                "暂无法判断",
                "待补充",
                "目前信息不足，无法可靠推荐科室。请描述具体不适部位、症状表现、持续时间、严重程度，以及是否伴有发热、出血、胸痛或呼吸困难。",
                0.0);
    }

    private TriageRuleResult emergency(String departmentId, String department, String advice) {
        return new TriageRuleResult(departmentId, department, "紧急", advice, 1.0);
    }

    private TriageRuleResult normal(String departmentId, String department, String advice) {
        return new TriageRuleResult(departmentId, department, "普通", advice, 0.8);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record TriageRuleResult(
            String departmentId,
            String department,
            String severity,
            String advice,
            double confidence) {
    }
}
