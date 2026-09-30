package my.hive.domain.aftersales.service;

import my.hive.shared.exception.BusinessException;
import java.util.List;
import java.util.Set;

public final class AfterSalesTreatmentPolicy {
    public static final Set<String> ACTIVE = Set.of("waiting_outbound", "processing", "waiting_follow_up");
    private AfterSalesTreatmentPolicy() {}

    public static void requireEditable(String status) {
        if (!Set.of("waiting_outbound", "processing").contains(status)) {
            throw new BusinessException("本次处理已完成，历史记录不可覆盖，请新增处理记录");
        }
    }

    /** Statuses must be in last-operation order, oldest first (including late follow-ups). */
    public static void requireClosable(List<String> statuses) {
        if (statuses.stream().anyMatch(ACTIVE::contains)) {
            throw new BusinessException("仍有处理记录未完成或未回访，不能结案");
        }
        if (!statuses.isEmpty() && !"resolved".equals(statuses.get(statuses.size() - 1))) {
            throw new BusinessException("最近一次回访尚未解决问题，请继续处理后再结案");
        }
    }
}
