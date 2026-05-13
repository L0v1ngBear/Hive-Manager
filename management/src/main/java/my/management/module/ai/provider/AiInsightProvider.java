package my.management.module.ai.provider;

import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.ai.model.entity.AiAdviceTrainingSample;

import java.util.List;

/**
 * Transformer AI 建议生成 Provider。
 *
 * <p>本接口用于隔离具体推理服务。业务层只认识经营快照、历史反馈样本和建议结构，
 * 不再依赖本地规则生成建议。</p>
 */
public interface AiInsightProvider {

    /**
     * 当前 Provider 是否可用。
     */
    boolean enabled();

    /**
     * 根据经营快照和近期反馈样本，生成 Transformer 建议。
     */
    List<DashboardAiAdviceVO> generate(AiBusinessSnapshotVO snapshot,
                                       List<DashboardAiAdviceVO> referenceAdvices,
                                       List<AiAdviceTrainingSample> trainingExamples);
}
