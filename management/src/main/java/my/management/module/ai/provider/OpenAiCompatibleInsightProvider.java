package my.management.module.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.management.module.ai.config.AiLlmProperties;
import my.management.module.ai.model.entity.AiAdviceTrainingSample;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 建议推理服务。
 *
 * <p>DeepSeek 兼容 OpenAI Chat Completions 协议，作为当前阶段推荐的商用推理入口。
 * 未来自训练 Transformer 上线后，由 SelfTrainedTransformerInsightProvider 优先承接。</p>
 * @since 1.0.0
 */
@Service
@Order(20)
public class OpenAiCompatibleInsightProvider implements AiInsightProvider {

    /**
     * AI大模型配置属性类
     */
    @Resource
    private AiLlmProperties properties;

    /**
     * Jackson JSON序列化/反序列化工具
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 判断当前大模型服务是否启用
     *
     * @return 已配置必要参数则返回true，否则false
     */
    @Override
    public boolean enabled() {
        return properties.deepseekReady();
    }

    /**
     * 生成AI智能经营建议（核心业务方法）
     * 1. 校验服务是否启用
     * 2. 构建请求参数
     * 3. 调用远程大模型接口
     * 4. 解析响应并提取JSON格式建议列表
     *
     * @param snapshot         业务经营快照数据
     * @param referenceAdvices 兼容保留参数，不作为本地规则基线使用
     * @param trainingExamples 本租户近期反馈样本，用于自训练模型在线校准
     * @return 包装好的Dashboard展示建议列表
     */
    @Override
    public List<DashboardAiAdviceVO> generate(AiBusinessSnapshotVO snapshot,
                                              List<DashboardAiAdviceVO> referenceAdvices,
                                              List<AiAdviceTrainingSample> trainingExamples) {
        // 服务未启用直接返回空列表
        if (!enabled()) {
            return List.of();
        }

        try {
            // 1. 构建AI接口请求体
            AiLlmProperties.Provider config = properties.deepseekConfig();
            String requestBody = objectMapper.writeValueAsString(buildRequest(config, snapshot, trainingExamples));

            // 2. 创建HTTP客户端
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .build();

            // 3. 构建POST请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 4. 发送请求并获取响应
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. 非200状态码直接返回空
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }

            // 6. 解析响应JSON，提取AI返回内容
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            // 7. 从返回内容中提取纯JSON数组字符串
            String jsonArray = extractJsonArray(content);
            if (jsonArray.isBlank()) {
                return List.of();
            }

            // 8. 反序列化为前端VO并返回
            List<DashboardAiAdviceVO> advices = objectMapper.readValue(jsonArray, new TypeReference<List<DashboardAiAdviceVO>>() {
            });
            return markSourceType(advices);
        } catch (Exception ignored) {
            // DeepSeek 推理异常不能拖垮业务大盘，失败时返回空建议。
            return List.of();
        }
    }

    /**
     * 构建大模型API请求参数
     *
     * @param snapshot        经营快照
     * @return 符合OpenAI格式的请求参数Map
     */
    private Map<String, Object> buildRequest(AiLlmProperties.Provider config,
                                             AiBusinessSnapshotVO snapshot,
                                             List<AiAdviceTrainingSample> trainingExamples) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", config.getModel());
        request.put("temperature", config.getTemperature());
        request.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userPrompt(config, snapshot, trainingExamples))
        ));
        return request;
    }

    /**
     * 系统提示词（定义AI角色、输出格式、约束规则）
     * <p>
     * 固定要求：
     * 1. 必须输出纯JSON数组，无多余内容
     * 2. 字段严格规范
     * 3. 基于真实数据，不编造
     * 4. 面向管理层提供业务洞察与决策建议
     * </p>
     *
     * @return 系统提示词
     */
    private String systemPrompt() {
        return """
                你是 Hive 蜂巢数字化工厂系统的 DeepSeek 经营建议模型。
                你必须基于用户提供的经营快照和历史反馈样本进行分析，不要编造不存在的数据。
                不允许套用本地规则模板，不允许输出与经营快照无关的泛泛建议。
                输出必须是 JSON 数组，不要 Markdown，不要解释性前后缀。
                每条建议字段必须包含：
                category, level, icon, title, summary, suggestion, route, priority,
                ownerDepartment, actionLabel, metricText, trackingHint, sourceType, confidence, reasoning,
                decisionType, riskScore, impactText, timeWindow, firstAction, reviewMetric。
                category 只能使用 inventory/order/delivery/customer/employee/quality/finance/operation/overview。
                level 只能使用 warning/info/success。sourceType 固定为 deepseek。
                riskScore 必须是 0-100 的整数，firstAction 必须是能立刻执行的一句话。
                建议必须面向老板和管理层，强调业务洞察、决策辅助、影响范围和行动闭环。
                历史反馈中 useful/resolved 代表可学习的正样本，irrelevant/ignored 代表需要降噪的负样本。
                """;
    }

    /**
     * 用户提示词（传入业务数据，要求AI生成建议）
     *
     * @param snapshot        经营快照
     * @param trainingExamples 近期已反馈训练样本
     * @return 用户提示词
     */
    private String userPrompt(AiLlmProperties.Provider config,
                              AiBusinessSnapshotVO snapshot,
                              List<AiAdviceTrainingSample> trainingExamples) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("maxAdvices", config.getMaxAdvices());
            payload.put("modelProvider", "deepseek");
            payload.put("futureTrainingTarget", "self_trained_transformer");
            payload.put("businessSnapshot", sanitizeSnapshot(snapshot));
            payload.put("recentFeedbackExamples", sanitizeTrainingExamples(trainingExamples));

            return "请基于以下全局经营快照生成最多 " + config.getMaxAdvices()
                    + " 条高价值决策建议。重点观察经营、客户、员工、质量和财务之间的交叉影响；"
                    + "结合 recentFeedbackExamples 学习本租户偏好，减少被忽略或判定无关的建议类型：\n"
                    + objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {
            return "请基于当前全局经营快照和历史反馈样本生成 DeepSeek 决策建议。";
        }
    }

    private List<Map<String, Object>> sanitizeTrainingExamples(List<AiAdviceTrainingSample> trainingExamples) {
        if (trainingExamples == null || trainingExamples.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> examples = new ArrayList<>();
        for (AiAdviceTrainingSample sample : trainingExamples.stream().limit(12).toList()) {
            if (sample == null || sample.getFeedbackType() == null || sample.getFeedbackType().isBlank()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", sample.getCategory());
            item.put("title", sample.getTitle());
            item.put("priority", sample.getPriority());
            item.put("confidence", sample.getConfidence());
            item.put("feedbackType", sample.getFeedbackType());
            item.put("feedbackText", sample.getFeedbackText());
            item.put("occurrenceCount", sample.getOccurrenceCount());
            examples.add(item);
        }
        return examples;
    }

    private List<DashboardAiAdviceVO> markSourceType(List<DashboardAiAdviceVO> advices) {
        if (advices == null || advices.isEmpty()) {
            return List.of();
        }
        return advices.stream()
                .filter(advice -> advice != null)
                .peek(advice -> advice.setSourceType("deepseek"))
                .toList();
    }

    /**
     * 清洗经营快照数据（移除敏感/无用字段）
     *
     * @param snapshot 原始经营快照
     * @return 清洗后的Map数据
     */
    private Map<String, Object> sanitizeSnapshot(AiBusinessSnapshotVO snapshot) {
        Map<String, Object> map = objectMapper.convertValue(snapshot, new TypeReference<Map<String, Object>>() {
        });
        // 移除租户编码，避免泄露
        map.remove("tenantCode");
        return map;
    }

    /**
     * 从AI返回内容中提取纯JSON数组
     * 处理AI可能返回的Markdown代码块、前后缀文字等
     *
     * @param content AI返回的原始内容
     * @return 纯JSON数组字符串，无则返回空
     */
    private String extractJsonArray(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        // 去除首尾空格
        String cleaned = content.trim();

        // 移除Markdown代码块标记 ```json ... ```
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }

        // 截取最外层 [] 之间的内容
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return "";
        }
        return cleaned.substring(start, end + 1);
    }
}
