package my.management.module.ai.model.vo;

import lombok.Data;

/**
 * 总览大盘 AI 建议展示对象，承载规则分析后的建议文本。
 */
@Data
public class DashboardAiAdviceVO {

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
}
