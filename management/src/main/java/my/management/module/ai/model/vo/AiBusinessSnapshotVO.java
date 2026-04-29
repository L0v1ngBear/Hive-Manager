package my.management.module.ai.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 全局分析快照。
 *
 * <p>该对象是 AI 建议的中间层：后端先把订单、库存、质量、客户、员工、审批等真实业务数据
 * 汇总成稳定指标，再交给规则分析器或大模型生成建议，避免页面或模型直接拼 SQL。</p>
 */
@Data
public class AiBusinessSnapshotVO {

    private String tenantCode;

    private LocalDateTime generatedAt;

    private OrderMetrics order = new OrderMetrics();

    private InventoryMetrics inventory = new InventoryMetrics();

    private QualityMetrics quality = new QualityMetrics();

    private CustomerMetrics customer = new CustomerMetrics();

    private EmployeeMetrics employee = new EmployeeMetrics();

    private FinanceMetrics finance = new FinanceMetrics();

    private TrendMetrics trend = new TrendMetrics();

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
        private Long activeCustomerCount30d = 0L;
        private Long newCustomerCount30d = 0L;
        private Long inactiveCustomerCount90d = 0L;
        private String topCustomerName30d;
        private BigDecimal topCustomerAmount30d = BigDecimal.ZERO;
    }

    @Data
    public static class EmployeeMetrics {
        private Long totalEmployeeCount = 0L;
        private Long activeEmployeeCount = 0L;
        private Long missingManagerCount = 0L;
        private Long attendanceExceptionCountToday = 0L;
        private Long lateCountToday = 0L;
        private Long pendingLeaveApprovalCount = 0L;
        private Long leaveRequestCount30d = 0L;
    }

    @Data
    public static class FinanceMetrics {
        private Long pendingFinanceApprovalCount = 0L;
        private BigDecimal pendingFinanceAmount = BigDecimal.ZERO;
    }

    @Data
    public static class TrendMetrics {
        private Long salesOrderCountPrevious30d = 0L;
        private Long productionOrderCountPrevious30d = 0L;
        private BigDecimal salesAmountPrevious30d = BigDecimal.ZERO;
        private Long badProductCountPrevious30d = 0L;
        private BigDecimal badProductLossPrevious30d = BigDecimal.ZERO;
        private Long activeCustomerCountPrevious30d = 0L;
        private Long newCustomerCountPrevious30d = 0L;
        private BigDecimal salesOrderGrowthRate = BigDecimal.ZERO;
        private BigDecimal salesAmountGrowthRate = BigDecimal.ZERO;
        private BigDecimal productionOrderGrowthRate = BigDecimal.ZERO;
        private BigDecimal badProductLossGrowthRate = BigDecimal.ZERO;
        private BigDecimal activeCustomerGrowthRate = BigDecimal.ZERO;
    }
}
