package my.management.module.ai.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 经营简报。
 *
 * <p>简报由当前用户可见的 AI 建议汇总生成，不绕过权限，也不替代人工决策。</p>
 */
@Data
public class AiAdviceDailyBriefVO {

    private LocalDateTime generatedAt;

    private String briefVersion;

    private String title;

    private String executiveSummary;

    private String topRiskTitle;

    private String topRiskSummary;

    private String firstAction;

    private Integer riskCount = 0;

    private Integer urgentActionCount = 0;

    private Integer watchCount = 0;

    private List<BriefItem> urgentActions = new ArrayList<>();

    private List<BriefItem> watchItems = new ArrayList<>();

    private List<BriefItem> reviewItems = new ArrayList<>();

    @Data
    public static class BriefItem {

        private String category;

        private String title;

        private String priority;

        private Integer riskScore;

        private String action;

        private String route;

        private String ownerDepartment;

        private String reviewMetric;
    }
}
