package my.management.module.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@Order(10)
public class OpenAiCompatibleInsightProvider implements AiInsightProvider {

    private static final String PROVIDER = "deepseek";
    private static final String ACTION = "chat-completions";

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

        long startNanos = System.nanoTime();
        String tenantSubject = tenantSubject(snapshot);
        AiLlmProperties.Provider config = null;
        try {
            config = properties.deepseekConfig();
            String requestBody = objectMapper.writeValueAsString(buildRequest(config, snapshot, trainingExamples));
            String cacheKey = externalApiGuardService.fingerprint(requestBody);
            String cachedResponse = externalApiGuardService.getCachedResponse(PROVIDER, ACTION, cacheKey);
            if (cachedResponse != null && !cachedResponse.isBlank()) {
                return parseResponseBody(cachedResponse);
            }

            externalApiGuardService.checkRateLimit(
                    PROVIDER,
                    ACTION,
                    tenantSubject,
                    deepseekMaxCallsPerWindow == null ? 30 : deepseekMaxCallsPerWindow,
                    Duration.ofSeconds(deepseekWindowSeconds == null ? 60 : Math.max(1, deepseekWindowSeconds))
            );

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds(config)))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .timeout(Duration.ofSeconds(timeoutSeconds(config)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMillis = elapsedMillis(startNanos);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                externalApiGuardService.recordCallEvent(PROVIDER, ACTION, "HTTP_ERROR", tenantSubject,
                        response.statusCode(), elapsedMillis, "智能建议服务返回异常",
                        Map.of("model", safeText(config.getModel())));
                return List.of();
            }

            List<DashboardAiAdviceVO> advices = parseResponseBody(response.body());
            externalApiGuardService.cacheResponse(
                    PROVIDER,
                    ACTION,
                    cacheKey,
                    response.body(),
                    Duration.ofSeconds(deepseekCacheSeconds == null ? 300 : Math.max(0, deepseekCacheSeconds))
            );
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("model", safeText(config.getModel()));
            detail.put("adviceCount", advices.size());
            detail.put("responseBytes", response.body() == null ? 0 : response.body().length());
            externalApiGuardService.recordCallEvent(PROVIDER, ACTION, "SUCCESS", tenantSubject,
                    response.statusCode(), elapsedMillis, "智能建议生成完成", detail);
            return advices;
        } catch (Exception exception) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("errorType", exception.getClass().getName());
            detail.put("errorMessage", exception.getMessage() == null ? "" : exception.getMessage());
            if (config != null) {
                detail.put("model", safeText(config.getModel()));
            }
            externalApiGuardService.recordCallEvent(PROVIDER, ACTION, "ERROR", tenantSubject,
                    null, elapsedMillis(startNanos), "智能建议服务调用失败", detail);
            log.warn("智能建议生成失败, tenant={}", tenantSubject, exception);
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
            // Compatible with occasional non-json-wrapper model output.
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
                你必须基于用户提供的经营快照和历史反馈样本分析，不要编造不存在的数据。
                不允许套用本地规则模板，不允许输出与经营快照无关的泛泛建议。
                输出必须是 JSON 对象，不要 Markdown，不要解释性前后缀，固定格式为 {"advices":[...]}。
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
            payload.put("modelProvider", PROVIDER);
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
            item.put("feedbackText", AiPayloadSanitizer.sanitizeFreeText(sample.getFeedbackText()));
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
                .peek(advice -> advice.setSourceType(PROVIDER))
                .toList();
    }

    private Map<String, Object> sanitizeSnapshot(AiBusinessSnapshotVO snapshot) {
        return AiPayloadSanitizer.sanitizeSnapshot(objectMapper, snapshot);
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

    private String tenantSubject(AiBusinessSnapshotVO snapshot) {
        if (snapshot == null || snapshot.getTenantCode() == null || snapshot.getTenantCode().isBlank()) {
            return "global";
        }
        return snapshot.getTenantCode().trim();
    }

    private int timeoutSeconds(AiLlmProperties.Provider config) {
        Integer timeout = config == null ? null : config.getTimeoutSeconds();
        return timeout == null ? 45 : Math.max(1, timeout);
    }

    private long elapsedMillis(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
