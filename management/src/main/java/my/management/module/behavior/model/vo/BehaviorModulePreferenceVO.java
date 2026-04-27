package my.management.module.behavior.model.vo;

import lombok.Data;

/**
 * 租户行为偏好聚合结果。
 */
@Data
public class BehaviorModulePreferenceVO {

    /**
     * 行为对应的业务维度，例如 inventory、order、customer、quality。
     */
    private String category;

    /**
     * 加权后的关注分，点击和通知打开权重大于普通曝光。
     */
    private Double behaviorScore;

    /**
     * 点击类行为次数。
     */
    private Long clickCount;

    /**
     * 总行为次数。
     */
    private Long totalCount;
}
