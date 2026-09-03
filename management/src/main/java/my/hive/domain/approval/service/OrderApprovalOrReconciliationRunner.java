package my.hive.domain.approval.service;

import my.hive.domain.approval.model.entity.ApprovalAuditorCandidate;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.shared.tenant.TenantIsolationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Completes legacy ORDER approvals which already have one approval after their persisted mode is
 * changed from AND to OR. The candidate query and completion are both idempotent: a completed
 * candidate set is closed and will not be selected on another startup.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OrderApprovalOrReconciliationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderApprovalOrReconciliationRunner.class);

    private static final Set<String> RECONCILIATION_PERMISSIONS = Set.of(
            PermissionCatalogV3.CODE_ORDER_SCOPE_TENANT,
            PermissionCatalogV3.CODE_ORDER_AUDIT_MATERIAL,
            PermissionCatalogV3.CODE_ORDER_AUDIT_SHIPMENT,
            PermissionCatalogV3.CODE_ORDER_AUDIT_CANCEL,
            "order:status:budgeting:view",
            "order:status:budget-completed:view",
            "order:status:pending-confirm:view",
            "order:status:pending-pay:view",
            "order:status:pending-material:view",
            "order:status:producing:view",
            "order:status:pending-ship:view",
            "order:status:shipped:view",
            "order:status:completed:view",
            "order:status:pending-cancel:view",
            "order:status:cancelled:view"
    );

    private final ApprovalAuditorCandidateService candidateService;
    private final ApprovalService approvalService;
    private final TenantIsolationSupport tenantIsolationSupport;

    public OrderApprovalOrReconciliationRunner(ApprovalAuditorCandidateService candidateService,
                                                ApprovalService approvalService,
                                                TenantIsolationSupport tenantIsolationSupport) {
        this.candidateService = candidateService;
        this.approvalService = approvalService;
        this.tenantIsolationSupport = tenantIsolationSupport;
    }

    @Override
    public void run(ApplicationArguments args) {
        final List<ApprovalAuditorCandidate> groups;
        try {
            groups = candidateService.findOrderGroupsReadyForOrReconciliation();
        } catch (RuntimeException ex) {
            log.error("Unable to inspect legacy ORDER approvals for OR reconciliation", ex);
            return;
        }
        if (groups.isEmpty()) {
            return;
        }

        int completed = 0;
        int failed = 0;
        for (ApprovalAuditorCandidate group : groups) {
            if (group == null || !StringUtils.hasText(group.getTenantCode())
                    || !StringUtils.hasText(group.getApprovalCode())
                    || group.getAuditorId() == null || group.getAuditorId() <= 0) {
                failed++;
                log.error("Skipped malformed legacy ORDER approval reconciliation row");
                continue;
            }
            String tenantCode = group.getTenantCode().trim();
            String approvalCode = group.getApprovalCode().trim();
            try {
                tenantIsolationSupport.bindTenantDatasource(tenantCode);
                TenantPermissionContext.init(tenantCode, group.getAuditorId(), RECONCILIATION_PERMISSIONS);
                if (approvalService.reconcileApprovedOrder(tenantCode, approvalCode)) {
                    completed++;
                }
            } catch (RuntimeException ex) {
                failed++;
                // Keep the candidate set active. The next startup can retry after the underlying
                // business prerequisite (for example complete shipping information) is corrected.
                log.error("Failed to reconcile legacy ORDER approval tenant={} approvalCode={}",
                        tenantCode, approvalCode, ex);
            } finally {
                TenantPermissionContext.clear();
                tenantIsolationSupport.clearTenantDatasource();
            }
        }
        log.info("Legacy ORDER OR reconciliation finished: selected={}, completed={}, failed={}",
                groups.size(), completed, failed);
    }
}
