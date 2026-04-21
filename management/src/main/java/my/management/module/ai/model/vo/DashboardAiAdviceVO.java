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
}
