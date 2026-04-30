package my.management.module.ai.model.vo;

import lombok.Data;

/**
 * 总览大盘 AI 建议展示对象，承载规则分析后的建议文本。
 */
@Data
public class DashboardAiAdviceVO {

    /**
     * 建议样本键，前端反馈时用它回写训练样本标签。
     */
    private String sampleKey;

    private String category;

    private String level;

    private String icon;

    private String title;

    private String summary;

    private String suggestion;

    private String route;

    /**
     * 管理优先级：P0/P1/P2/P3，前端用于排序和视觉强调。
     */
    private String priority;

    /**
     * 建议牵头处理的部门或角色。
     */
    private String ownerDepartment;

    /**
     * 按钮行动文案，例如“查看库存”“跟进订单”。
     */
    private String actionLabel;

    /**
     * 该建议背后的数据口径或关键指标。
     */
    private String metricText;

    /**
     * 建议生成时间，帮助管理层判断数据新鲜度。
     */
    private String generatedAt;

    /**
     * 跟进提示，描述下一步如何形成闭环。
     */
    private String trackingHint;

    /**
     * 建议来源：local_rules 表示本地规则分析，llm 表示大模型分析。
     */
    private String sourceType;

    /**
     * 置信度，0-100。用于后续区分强规则结论和大模型推断结论。
     */
    private Integer confidence;

    /**
     * 分析依据，说明该建议为什么成立。
     */
    private String reasoning;

    /**
     * 决策类型，例如风险预警、增长机会、效率优化或运营治理。
     */
    private String decisionType;

    /**
     * 综合风险分，0-100。分数越高代表越需要管理层优先处理。
     */
    private Integer riskScore;

    /**
     * 影响范围，描述该建议可能影响的业务结果。
     */
    private String impactText;

    /**
     * 建议处理窗口，例如今日内、本周内或持续观察。
     */
    private String timeWindow;

    /**
     * 第一动作，给管理层一个最小可执行切入点。
     */
    private String firstAction;

    /**
     * 复盘指标，帮助判断建议是否真正形成闭环。
     */
    private String reviewMetric;

    /**
     * 管理层应该先回答的关键决策问题。
     */
    private String decisionQuestion;

    /**
     * 跨部门协同路径，避免建议只停留在提醒层。
     */
    private String collaborationPath;

    /**
     * 风险升级规则，明确什么时候需要更高权限负责人介入。
     */
    private String escalationRule;

    /**
     * 预防动作，用于把一次性处理沉淀成长期机制。
     */
    private String preventionAction;

    /**
     * 建议复盘节奏，例如日会、周会或月度经营会。
     */
    private String meetingCadence;

    /**
     * 建议可见层级，用于前端区分一线提醒和高层经营建议。
     */
    private String visibilityTier;

    /**
     * 生成该建议的规则编码，用于问题追踪和后续策略回退。
     */
    private String ruleCode;

    /**
     * 规则版本，帮助判断建议来自哪一版规则体系。
     */
    private String ruleVersion;

    /**
     * 策略版本，区分当前线上策略和候选影子策略。
     */
    private String strategyVersion;

    /**
     * 评估模式：ONLINE 表示当前线上可见建议，SHADOW 表示只参与对比评估。
     */
    private String evaluationMode;
}
