package my.hive.domain.approval.service;

import my.hive.shared.exception.BusinessException;
import my.hive.domain.approval.mapper.ApprovalAuditorCandidateMapper;
import my.hive.domain.approval.model.entity.ApprovalAuditorCandidate;
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
import static org.mockito.ArgumentMatchers.anyInt;
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
    void orModeApprovesAsSoonAsOneAuditorApproves() {
        ApprovalAuditorCandidate first = candidate(1L, "tenant-a", 11L, 1, 0);
        ApprovalAuditorCandidate second = candidate(2L, "tenant-a", 12L, 1, 0);
        first.setApprovalMode("OR");
        second.setApprovalMode("OR");
        ApprovalAuditorCandidate approved = candidate(1L, "tenant-a", 11L, 1, 1);
        ApprovalAuditorCandidate pending = candidate(2L, "tenant-a", 12L, 1, 0);
        approved.setApprovalMode("OR");
        pending.setApprovalMode("OR");
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(first, second), List.of(approved, pending));
        when(mapper.updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(11L), eq(1), eq("ok"), any(LocalDateTime.class)))
                .thenReturn(1);

        ApprovalAuditorCandidateService.ApprovalDecision result = subject.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 11L, true, "ok");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED, result);
    }

    @Test
    void resolvesMigratedOrGroupWithApprovedAndPendingCandidatesAsApproved() {
        ApprovalAuditorCandidate approved = candidate(1L, "tenant-a", 11L, 1, 1);
        ApprovalAuditorCandidate pending = candidate(2L, "tenant-a", 12L, 1, 0);
        approved.setApprovalMode("OR");
        pending.setApprovalMode("OR");
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(approved, pending));

        ApprovalAuditorCandidateService.ApprovalDecision result = subject
                .resolveActiveDecisionForUpdate("tenant-a", "ORDER", "sales:SO-100");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.APPROVED, result);
        verify(mapper, never()).updatePendingDecision(
                any(), any(), any(), any(), anyInt(), any(), any(LocalDateTime.class));
    }

    @Test
    void orModeKeepsWaitingAfterOneAuditorRejects() {
        ApprovalAuditorCandidate first = candidate(1L, "tenant-a", 11L, 1, 0);
        ApprovalAuditorCandidate second = candidate(2L, "tenant-a", 12L, 1, 0);
        first.setApprovalMode("OR");
        second.setApprovalMode("OR");
        ApprovalAuditorCandidate rejected = candidate(1L, "tenant-a", 11L, 1, 2);
        ApprovalAuditorCandidate pending = candidate(2L, "tenant-a", 12L, 1, 0);
        rejected.setApprovalMode("OR");
        pending.setApprovalMode("OR");
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(first, second), List.of(rejected, pending));
        when(mapper.updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(11L), eq(2), eq("no"), any(LocalDateTime.class)))
                .thenReturn(1);

        ApprovalAuditorCandidateService.ApprovalDecision result = subject.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 11L, false, "no");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.PENDING, result);
    }

    @Test
    void orModeRejectsOnlyAfterAllAuditorsReject() {
        ApprovalAuditorCandidate first = candidate(1L, "tenant-a", 11L, 1, 2);
        ApprovalAuditorCandidate second = candidate(2L, "tenant-a", 12L, 1, 0);
        ApprovalAuditorCandidate rejectedFirst = candidate(1L, "tenant-a", 11L, 1, 2);
        ApprovalAuditorCandidate rejectedSecond = candidate(2L, "tenant-a", 12L, 1, 2);
        for (ApprovalAuditorCandidate candidate : List.of(first, second, rejectedFirst, rejectedSecond)) {
            candidate.setApprovalMode("OR");
        }
        when(mapper.selectApprovalForUpdate("tenant-a", "ORDER", "sales:SO-100"))
                .thenReturn(List.of(first, second), List.of(rejectedFirst, rejectedSecond));
        when(mapper.updatePendingDecision(
                eq("tenant-a"), eq("ORDER"), eq("sales:SO-100"), eq(12L), eq(2), eq("no"), any(LocalDateTime.class)))
                .thenReturn(1);

        ApprovalAuditorCandidateService.ApprovalDecision result = subject.recordDecision(
                "tenant-a", "ORDER", "sales:SO-100", 12L, false, "no");

        assertEquals(ApprovalAuditorCandidateService.ApprovalDecision.REJECTED, result);
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

    @Test
    void relatedApprovalCodesRetainClosedHistoryAndRemoveDuplicates() {
        ApprovalAuditorCandidate closed = candidate(4L, "tenant-a", 11L, 2, 1);
        closed.setApprovalType("LEAVE");
        closed.setApprovalCode("LV-100");
        ApprovalAuditorCandidate activeDuplicate = candidate(5L, "tenant-a", 11L, 1, 0);
        activeDuplicate.setApprovalType("LEAVE");
        activeDuplicate.setApprovalCode("LV-100");
        ApprovalAuditorCandidate rejected = candidate(6L, "tenant-a", 11L, 2, 2);
        rejected.setApprovalType("LEAVE");
        rejected.setApprovalCode("LV-101");
        when(mapper.selectList(any())).thenReturn(List.of(closed, activeDuplicate, rejected));

        assertEquals(List.of("LV-100", "LV-101"),
                subject.findRelatedApprovalCodes("tenant-a", "LEAVE", 11L));
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
