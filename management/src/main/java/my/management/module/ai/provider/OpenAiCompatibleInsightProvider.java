package my.management.module.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.common.external.ExternalApiGuardService;
import my.management.module.ai.config.AiLlmProperties;
import my.management.module.ai.model.entity.AiAdviceTrainingSample;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@Order(10)
public class OpenAiCompatibleInsightProvider implements AiInsightProvider {

    @Resource
    private AiLlmProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ExternalApiGuardService externalApiGuardService;

    @Value("${external-api.guard.deepseek.max-calls-per-window:30}")
    private Integer deepseekMaxCallsPerWindow;

    @Value("${external-api.guard.deepseek.window-seconds:60}")
    private Integer deepseekWindowSeconds;

    @Value("${external-api.guard.deepseek.cache-seconds:300}")
    private Integer deepseekCacheSeconds;

    @Override
    public boolean enabled() {
        return properties.deepseekReady();
    }

    @Override
    public List<DashboardAiAdviceVO> generate(AiBusinessSnapshotVO snapshot,
                                              List<DashboardAiAdviceVO> referenceAdvices,
                                              List<AiAdviceTrainingSample> trainingExamples) {
        if (!enabled()) {
            return List.of();
        }

        try {
            AiLlmProperties.Provider config = properties.deepseekConfig();
            String requestBody = objectMapper.writeValueAsString(buildRequest(config, snapshot, trainingExamples));
            String cacheKey = externalApiGuardService.fingerprint(requestBody);
            String cachedResponse = externalApiGuardService.getCachedResponse("deepseek", "chat-completions", cacheKey);
            if (cachedResponse != null && !cachedResponse.isBlank()) {
                return parseResponseBody(cachedResponse);
            }

            externalApiGuardService.checkRateLimit(
                    "deepseek",
                    "chat-completions",
                    snapshot == null || snapshot.getTenantCode() == null ? "global" : snapshot.getTenantCode(),
                    deepseekMaxCallsPerWindow == null ? 30 : deepseekMaxCallsPerWindow,
                    Duration.ofSeconds(deepseekWindowSeconds == null ? 60 : Math.max(1, deepseekWindowSeconds))
            );

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }

            externalApiGuardService.cacheResponse(
                    "deepseek",
                    "chat-completions",
                    cacheKey,
                    response.body(),
                    Duration.ofSeconds(deepseekCacheSeconds == null ? 300 : Math.max(0, deepseekCacheSeconds))
            );
            return parseResponseBody(response.body());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<DashboardAiAdviceVO> parseResponseBody(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        String cleaned = cleanJsonContent(content);
        if (cleaned.isBlank()) {
            return List.of();
        }
        try {
            JsonNode contentNode = objectMapper.readTree(cleaned);
            if (contentNode.isArray()) {
                return markSourceType(objectMapper.readValue(contentNode.toString(), new TypeReference<List<DashboardAiAdviceVO>>() {
                }));
            }
            JsonNode advicesNode = contentNode.path("advices");
            if (advicesNode.isArray()) {
                return markSourceType(objectMapper.readValue(advicesNode.toString(), new TypeReference<List<DashboardAiAdviceVO>>() {
                }));
            }
        } catch (Exception ignored) {
            // 兼容未开启 JSON 模式或模型偶发输出解释文本的情况，下面继续尝试截取数组。
        }
        String jsonArray = extractJsonArray(cleaned);
        if (!jsonArray.isBlank()) {
            return markSourceType(objectMapper.readValue(jsonArray, new TypeReference<List<DashboardAiAdviceVO>>() {
            }));
        }
        return List.of();
    }

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
        request.put("response_format", Map.of("type", "json_object"));
        return request;
    }

    private String systemPrompt() {
        return """
                你是 Hive 蜂巢数字化工厂系统的 DeepSeek 经营建议模型。
                你必须基于用户提供的经营快照和历史反馈样本进行分析，不要编造不存在的数据。
                不允许套用本地规则模板，不允许输出与经营快照无关的泛泛建议。
                输出必须是 JSON 对象，不要 Markdown，不要解释性前后缀，格式固定为 {"advices":[...]}。
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

    private Map<String, Object> sanitizeSnapshot(AiBusinessSnapshotVO snapshot) {
        if (snapshot == null) {
            return Map.of();
        }
        Map<String, Object> map = objectMapper.convertValue(snapshot, new TypeReference<Map<String, Object>>() {
        });
        map.remove("tenantCode");
        return map;
    }

    private String cleanJsonContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return cleaned;
    }

    private String extractJsonArray(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String cleaned = cleanJsonContent(content);
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return "";
        }
        return cleaned.substring(start, end + 1);
    }
}
