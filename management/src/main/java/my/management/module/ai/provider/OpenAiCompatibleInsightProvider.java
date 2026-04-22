package my.management.module.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.management.module.ai.config.AiLlmProperties;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
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
 * 兼容 OpenAI 协议的大模型建议生成服务实现类
 * 支持接入 DeepSeek / Qwen / OpenAI 等所有兼容 Chat Completions 接口的大模型
 * 通过配置 endpoint、model、apiKey 即可快速切换不同的大模型服务
 * 核心职责：根据业务经营快照数据，调用AI接口生成Dashboard展示的智能经营建议
 * @since 1.0.0
 */
@Service
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
        return properties.isReady();
    }

    /**
     * 生成AI智能经营建议（核心业务方法）
     * 1. 校验服务是否启用
     * 2. 构建请求参数
     * 3. 调用远程大模型接口
     * 4. 解析响应并提取JSON格式建议列表
     *
     * @param snapshot        业务经营快照数据
     * @param baselineAdvices 本地规则建议（用于去重）
     * @return 包装好的Dashboard展示建议列表
     */
    @Override
    public List<DashboardAiAdviceVO> generate(AiBusinessSnapshotVO snapshot, List<DashboardAiAdviceVO> baselineAdvices) {
        // 服务未启用直接返回空列表
        if (!enabled()) {
            return List.of();
        }

        try {
            // 1. 构建AI接口请求体
            String requestBody = objectMapper.writeValueAsString(buildRequest(snapshot, baselineAdvices));

            // 2. 创建HTTP客户端
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .build();

            // 3. 构建POST请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEndpoint()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
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
            return objectMapper.readValue(jsonArray, new TypeReference<List<DashboardAiAdviceVO>>() {
            });
        } catch (Exception ignored) {
            // 异常吞掉，保证服务不崩溃，返回空建议
            return List.of();
        }
    }

    /**
     * 构建大模型API请求参数
     *
     * @param snapshot        经营快照
     * @param baselineAdvices 本地规则建议
     * @return 符合OpenAI格式的请求参数Map
     */
    private Map<String, Object> buildRequest(AiBusinessSnapshotVO snapshot, List<DashboardAiAdviceVO> baselineAdvices) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getModel());
        request.put("temperature", properties.getTemperature());
        request.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userPrompt(snapshot, baselineAdvices))
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
                你是 Hive 蜂巢数字化工厂系统的纺织行业经营分析顾问。
                你必须基于用户提供的经营快照和本地规则基线进行分析，不要编造不存在的数据。
                输出必须是 JSON 数组，不要 Markdown，不要解释性前后缀。
                每条建议字段必须包含：
                category, level, icon, title, summary, suggestion, route, priority,
                ownerDepartment, actionLabel, metricText, trackingHint, sourceType, confidence, reasoning。
                category 只能使用 inventory/order/delivery/customer/quality/finance/operation/overview。
                level 只能使用 warning/info/success。sourceType 固定为 llm。
                建议必须面向老板和管理层，强调业务洞察、决策辅助和行动闭环。
                """;
    }

    /**
     * 用户提示词（传入业务数据，要求AI生成建议）
     *
     * @param snapshot        经营快照
     * @param baselineAdvices 本地规则建议（用于去重）
     * @return 用户提示词
     */
    private String userPrompt(AiBusinessSnapshotVO snapshot, List<DashboardAiAdviceVO> baselineAdvices) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("maxAdvices", properties.getMaxAdvices());
            payload.put("businessSnapshot", sanitizeSnapshot(snapshot));
            payload.put("baselineAdviceTitles", baselineAdvices.stream()
                    .map(DashboardAiAdviceVO::getTitle)
                    .filter(title -> title != null && !title.isBlank())
                    .limit(8)
                    .toList());

            return "请基于以下经营快照生成最多 " + properties.getMaxAdvices()
                    + " 条额外的高价值经营建议，避免与 baselineAdviceTitles 重复：\n"
                    + objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {
            return "请基于当前经营快照生成经营建议。";
        }
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