package my.hive.domain.approval.service;

import my.hive.domain.approval.model.entity.ApprovalAuditorCandidate;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.tenant.TenantIsolationSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApprovalOrReconciliationRunnerTest {

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void startupReconcilesOnlySelectedOrderGroupAndClearsTenantContext() throws Exception {
        ApprovalAuditorCandidateService candidateService = mock(ApprovalAuditorCandidateService.class);
        ApprovalService approvalService = mock(ApprovalService.class);
        TenantIsolationSupport isolationSupport = mock(TenantIsolationSupport.class);
        ApprovalAuditorCandidate group = new ApprovalAuditorCandidate();
        group.setTenantCode("tenant-a");
        group.setApprovalType("ORDER");
        group.setApprovalCode("sales:SO-100");
        group.setAuditorId(11L);
        when(candidateService.findOrderGroupsReadyForOrReconciliation()).thenReturn(List.of(group));
        when(approvalService.reconcileApprovedOrder("tenant-a", "sales:SO-100")).thenReturn(true);
        OrderApprovalOrReconciliationRunner runner = new OrderApprovalOrReconciliationRunner(
                candidateService, approvalService, isolationSupport);

        runner.run(null);

        verify(isolationSupport).bindTenantDatasource("tenant-a");
        verify(approvalService).reconcileApprovedOrder("tenant-a", "sales:SO-100");
        verify(isolationSupport).clearTenantDatasource();
        assertNull(TenantPermissionContext.getTenantCode());
    }
}
