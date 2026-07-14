package my.management.module.approval.service;

import my.hive.shared.exception.BusinessException;
import my.management.module.approval.mapper.ApprovalAuditorCandidateMapper;
import my.management.module.approval.model.entity.ApprovalAuditorCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalAuditorCandidateServiceTest {

    @Mock
    private ApprovalAuditorCandidateMapper mapper;

    private ApprovalAuditorCandidateService subject;

    @BeforeEach
    void setUp() {
        subject = new ApprovalAuditorCandidateService();
        ReflectionTestUtils.setField(subject, "approvalAuditorCandidateMapper", mapper);
    }

    @Test
    void recordsDecisionBetweenTwoLockingCurrentReads() {
        List<ApprovalAuditorCandidate> before = List.of(
                candidate(1L, "tenant-a", 11L, 1, 0),
                candidate(2L, "tenant-a", 12L, 1, 0));
        List<ApprovalAuditorCandidate> after = List.of(
                candidate(1L, "tenant-a", 11L, 1, 1),
                candidate(2L, "tenant-a", 12L, 1, 0));
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(before, after);
        when(mapper.updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(11L), eq(1), eq("ok"), any(LocalDateTime.class)))
                .thenReturn(1);

        ApprovalAuditorCandidateService.ApprovalDecision result = subject.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 11L, true, "ok");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.PENDING, result);
        InOrder order = inOrder(mapper);
        order.verify(mapper).selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100");
        order.verify(mapper).updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(11L), eq(1), eq("ok"), any(LocalDateTime.class));
        order.verify(mapper).selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100");
    }

    @Test
    void lastApproverObservesAllApprovedAndMayAdvance() {
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(
                        List.of(candidate(1L, "tenant-a", 11L, 1, 1), candidate(2L, "tenant-a", 12L, 1, 0)),
                        List.of(candidate(1L, "tenant-a", 11L, 1, 1), candidate(2L, "tenant-a", 12L, 1, 1)));
        when(mapper.updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(12L), eq(1), eq("ok"), any(LocalDateTime.class)))
                .thenReturn(1);

        ApprovalAuditorCandidateService.ApprovalDecision result = subject.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 12L, true, "ok");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED, result);
    }

    @Test
    void anyRejectedDecisionWinsOverPendingAndApproval() {
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "production:PO-100"))
                .thenReturn(
                        List.of(candidate(1L, "tenant-a", 11L, 1, 0), candidate(2L, "tenant-a", 12L, 1, 0)),
                        List.of(candidate(1L, "tenant-a", 11L, 1, 2), candidate(2L, "tenant-a", 12L, 1, 0)));
        when(mapper.updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("production:PO-100"), eq(11L), eq(2), eq("no"), any(LocalDateTime.class)))
                .thenReturn(1);

        ApprovalAuditorCandidateService.ApprovalDecision result = subject.recordDecision(
                "tenant-a", "ORDER", "production:PO-100", 11L, false, "no");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.REJECTED, result);
    }

    @Test
    void zeroAffectedRowsRejectsDuplicateOrStaleDecision() {
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(candidate(1L, "tenant-a", 11L, 1, 0)));
        when(mapper.updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(11L), eq(1), eq("ok"), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThrows(BusinessException.class, () -> subject.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 11L, true, "ok"));

        verify(mapper).selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100");
    }

    @Test
    void closedApprovalHistoryRejectsCommandInsteadOfFallingBackToLegacyFlow() {
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(candidate(1L, "tenant-a", 11L, 2, 1)));

        assertThrows(BusinessException.class, () -> subject.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 11L, true, "ok"));

        verify(mapper, never()).updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(11L), eq(1), eq("ok"), any(LocalDateTime.class));
    }

    @Test
    void noCandidateHistoryKeepsLegacyApprovalCompatibility() {
        when(mapper.selectApprovalForUpdate("tenant-a", "LEAVE", "LV-100")).thenReturn(List.of());

        ApprovalAuditorCandidateService.ApprovalDecision result = subject.recordDecision(
                "tenant-a", "LEAVE", "LV-100", 11L, true, "ok");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.LEGACY, result);
        verify(mapper, never()).updatePendingDecision(
                eq("tenant-a"), eq("LEAVE"), eq("LV-100"), eq(11L), eq(1), eq("ok"), any(LocalDateTime.class));
    }

    @Test
    void tenantIsPartOfBothLockAndConditionalUpdate() {
        when(mapper.selectApprovalForUpdate("tenant-b", "ORDER", "sales:SO-100"))
                .thenReturn(
                        List.of(candidate(3L, "tenant-b", 21L, 1, 0)),
                        List.of(candidate(3L, "tenant-b", 21L, 1, 1)));
        when(mapper.updatePendingDecision(
                eq("tenant-b"), eq("ORDER"), eq("sales:SO-100"), eq(21L), eq(1), eq("ok"), any(LocalDateTime.class)))
                .thenReturn(1);

        subject.recordDecision("tenant-b", "ORDER", "sales:SO-100", 21L, true, "ok");

        verify(mapper).updatePendingDecision(
                eq("tenant-b"), eq("ORDER"), eq("sales:SO-100"), eq(21L), eq(1), eq("ok"), any(LocalDateTime.class));
        verify(mapper, never()).selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100");
    }

    private ApprovalAuditorCandidate candidate(Long id,
                                                String tenantCode,
                                                Long auditorId,
                                                int status,
                                                int auditStatus) {
        ApprovalAuditorCandidate candidate = new ApprovalAuditorCandidate();
        candidate.setId(id);
        candidate.setTenantCode(tenantCode);
        candidate.setApprovalType("ORDER");
        candidate.setApprovalCode("sales:SO-100");
        candidate.setAuditorId(auditorId);
        candidate.setStatus(status);
        candidate.setAuditStatus(auditStatus);
        return candidate;
    }
}
