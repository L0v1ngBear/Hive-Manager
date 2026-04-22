package my.management.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 经营分析快照。
 *
 * <p>该对象是 AI 建议的中间层：先把数据库里的订单、库存、次品、客户和审批数据
 * 汇总成稳定的业务指标，再交给规则分析器或大模型生成建议，避免页面或模型直接拼 SQL。</p>
 */
@Data
public class AiBusinessSnapshotVO {

    private String tenantCode;

    private LocalDateTime generatedAt;

    private OrderMetrics order = new OrderMetrics();

    private InventoryMetrics inventory = new InventoryMetrics();

    private QualityMetrics quality = new QualityMetrics();

    private CustomerMetrics customer = new CustomerMetrics();

    private FinanceMetrics finance = new FinanceMetrics();

    @Data
    public static class OrderMetrics {
        private Long salesOrderCount30d = 0L;
        private Long productionOrderCount30d = 0L;
        private Long unshippedDueSoonCount = 0L;
        private Long producingCount = 0L;
        private BigDecimal salesAmount30d = BigDecimal.ZERO;
    }

    @Data
    public static class InventoryMetrics {
        private BigDecimal totalMeters = BigDecimal.ZERO;
        private Long lowStockModelCount = 0L;
        private Long badClothCount = 0L;
    }

    @Data
    public static class QualityMetrics {
        private Long badProductCount30d = 0L;
        private BigDecimal badProductLoss30d = BigDecimal.ZERO;
        private Long pendingBadProductCount = 0L;
    }

    @Data
    public static class CustomerMetrics {
        private Long customerCount = 0L;
        private Long activeCustomerCount90d = 0L;
    }

    @Data
    public static class FinanceMetrics {
        private Long pendingFinanceApprovalCount = 0L;
        private BigDecimal pendingFinanceAmount = BigDecimal.ZERO;
    }
}
