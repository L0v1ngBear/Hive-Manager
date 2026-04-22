package my.management.module.ai.provider;

import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;

import java.util.List;

/**
 * AI 建议生成 Provider。
 *
 * <p>本接口用于隔离具体模型供应商。业务层只认识经营快照和建议结构，
 * 不关心底层是本地规则、OpenAI、DeepSeek 还是 Qwen。</p>
 */
public interface AiInsightProvider {

    /**
     * 当前 Provider 是否可用。
     */
    boolean enabled();

    /**
     * 根据经营快照和本地规则基线，生成增强建议。
     */
    List<DashboardAiAdviceVO> generate(AiBusinessSnapshotVO snapshot, List<DashboardAiAdviceVO> baselineAdvices);
}
