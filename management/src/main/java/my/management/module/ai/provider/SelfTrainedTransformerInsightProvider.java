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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 未来自训练 Transformer 远程推理服务入口。
 *
 * <p>当前不要求本地部署模型；该 Provider 默认关闭，只在未来配置远程推理 endpoint 后启用。
 * 预期由 Python/FastAPI/Triton 等服务承接训练后的模型，Java 只传入经营快照和反馈样本摘要。
 * 响应既支持原生 {"advices": [...]}，也兼容 OpenAI choices.message.content 返回 JSON 数组。</p>
 */
@Service
@Order(10)
public class SelfTrainedTransformerInsightProvider implements AiInsightProvider {

    @Resource
    private AiLlmProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean enabled() {
        return properties.selfTrainedReady();
    }

    @Override
    public List<DashboardAiAdviceVO> generate(AiBusinessSnapshotVO snapshot,
                                              List<DashboardAiAdviceVO> referenceAdvices,
                                              List<AiAdviceTrainingSample> trainingExamples) {
        if (!enabled()) {
            return List.of();
        }
        AiLlmProperties.Provider config = properties.getSelfTrained();
        try {
            String requestBody = objectMapper.writeValueAsString(buildRequest(config, snapshot, trainingExamples));
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
            return parseAdvices(response.body());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> buildRequest(AiLlmProperties.Provider config,
                                             AiBusinessSnapshotVO snapshot,
                                             List<AiAdviceTrainingSample> trainingExamples) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.getModel());
        payload.put("maxAdvices", config.getMaxAdvices());
        payload.put("temperature", config.getTemperature());
        payload.put("task", "hive_management_advice_generation");
        payload.put("modelArchitecture", "self_trained_transformer");
        payload.put("businessSnapshot", sanitizeSnapshot(snapshot));
        payload.put("recentFeedbackExamples", sanitizeTrainingExamples(trainingExamples));
        payload.put("requiredOutput", "DashboardAiAdviceVO[]");
        return payload;
    }

    private Map<String, Object> sanitizeSnapshot(AiBusinessSnapshotVO snapshot) {
        Map<String, Object> map = objectMapper.convertValue(snapshot, new TypeReference<Map<String, Object>>() {
        });
        map.remove("tenantCode");
        return map;
    }

    private List<Map<String, Object>> sanitizeTrainingExamples(List<AiAdviceTrainingSample> trainingExamples) {
        if (trainingExamples == null || trainingExamples.isEmpty()) {
            return List.of();
        }
        return trainingExamples.stream()
                .filter(sample -> sample != null && sample.getFeedbackType() != null && !sample.getFeedbackType().isBlank())
                .limit(20)
                .map(sample -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("category", sample.getCategory());
                    item.put("title", sample.getTitle());
                    item.put("sourceType", sample.getSourceType());
                    item.put("priority", sample.getPriority());
                    item.put("confidence", sample.getConfidence());
                    item.put("feedbackType", sample.getFeedbackType());
                    item.put("feedbackText", sample.getFeedbackText());
                    item.put("occurrenceCount", sample.getOccurrenceCount());
                    return item;
                })
                .toList();
    }

    private List<DashboardAiAdviceVO> parseAdvices(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode advices = root.path("advices");
        if (advices.isArray()) {
            return markSourceType(objectMapper.readValue(advices.toString(), new TypeReference<List<DashboardAiAdviceVO>>() {
            }));
        }
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        String jsonArray = extractJsonArray(content);
        if (jsonArray.isBlank()) {
            return List.of();
        }
        return markSourceType(objectMapper.readValue(jsonArray, new TypeReference<List<DashboardAiAdviceVO>>() {
        }));
    }

    private List<DashboardAiAdviceVO> markSourceType(List<DashboardAiAdviceVO> advices) {
        if (advices == null || advices.isEmpty()) {
            return List.of();
        }
        return advices.stream()
                .filter(advice -> advice != null)
                .peek(advice -> advice.setSourceType("transformer"))
                .toList();
    }

    private String extractJsonArray(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return "";
        }
        return cleaned.substring(start, end + 1);
    }
}
