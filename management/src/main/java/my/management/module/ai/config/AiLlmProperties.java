package my.management.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 大模型配置。
 *
 * <p>默认关闭，保证没有 API Key 时系统仍然使用本地规则分析。
 * 后续接入 OpenAI、DeepSeek、Qwen 等 OpenAI-Compatible 接口时，只需要配置环境变量。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class AiLlmProperties {

    /**
     * 是否启用大模型增强分析。
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

    public boolean isReady() {
        return Boolean.TRUE.equals(enabled)
                && endpoint != null && !endpoint.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank();
    }
}
