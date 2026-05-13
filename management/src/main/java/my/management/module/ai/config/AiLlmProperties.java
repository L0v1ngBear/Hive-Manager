package my.management.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模型配置。
 *
 * <p>当前预留两套推理入口：DeepSeek 直接调用官方 API，用于近期商用落地；
 * 自训练 Transformer 仅作为未来远程推理接口预留，默认关闭，不要求本地部署。
 * Java 端只负责调用推理服务和沉淀训练样本，不在 JVM 内训练 Transformer。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class AiLlmProperties {

    /**
     * 兼容旧配置：是否启用 DeepSeek/OpenAI-Compatible 推理。
     */
    private Boolean enabled = false;

    /**
     * OpenAI-Compatible Chat Completions 完整地址。
     */
    private String endpoint;

    /**
     * 大模型 API Key。
     */
    private String apiKey;

    /**
     * 模型名称，例如 gpt-5.4、deepseek-reasoner、qwen-plus。
     */
    private String model;

    /**
     * 最多生成多少条大模型建议。
     */
    private Integer maxAdvices = 3;

    /**
     * 请求超时时间，单位秒。
     */
    private Integer timeoutSeconds = 30;

    /**
     * 温度参数，经营分析默认保持低随机性。
     */
    private Double temperature = 0.2;

    /**
     * 当前商用推荐入口：DeepSeek 官方 OpenAI-Compatible Chat Completions。
     */
    private Provider deepseek = new Provider();

    /**
     * 未来自训练 Transformer 远程推理服务入口，通常由 Python/FastAPI/Triton 提供，默认关闭。
     */
    private Provider selfTrained = new Provider();

    public boolean isReady() {
        return Boolean.TRUE.equals(enabled)
                && endpoint != null && !endpoint.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank();
    }

    public boolean deepseekReady() {
        return deepseek.ready() || isReady();
    }

    public Provider deepseekConfig() {
        if (deepseek.ready()) {
            return deepseek;
        }
        Provider legacy = new Provider();
        legacy.setEnabled(enabled);
        legacy.setEndpoint(endpoint);
        legacy.setApiKey(apiKey);
        legacy.setModel(model);
        legacy.setMaxAdvices(maxAdvices);
        legacy.setTimeoutSeconds(timeoutSeconds);
        legacy.setTemperature(temperature);
        return legacy;
    }

    public boolean selfTrainedReady() {
        return selfTrained.ready();
    }

    @Data
    public static class Provider {

        private Boolean enabled = false;

        private String endpoint;

        private String apiKey;

        private String model;

        private Integer maxAdvices = 4;

        private Integer timeoutSeconds = 45;

        private Double temperature = 0.2;

        public boolean ready() {
            return Boolean.TRUE.equals(enabled)
                    && endpoint != null && !endpoint.isBlank()
                    && apiKey != null && !apiKey.isBlank()
                    && model != null && !model.isBlank();
        }
    }
}
