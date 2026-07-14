package my.hive.domain.approval;

import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.approval.service.ApprovalService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnifiedApprovalServiceTest {

    @Test
    void concurrentAuditorOutcomesRemainExplicit() {
        assertEquals(4, ApprovalAuditorCandidateService.ApprovalDecision.values().length);
        assertNotNull(ApprovalAuditorCandidateService.ApprovalDecision.valueOf("PENDING"));
        assertNotNull(ApprovalAuditorCandidateService.ApprovalDecision.valueOf("APPROVED"));
        assertNotNull(ApprovalAuditorCandidateService.ApprovalDecision.valueOf("REJECTED"));
        assertNotNull(ApprovalAuditorCandidateService.ApprovalDecision.valueOf("LEGACY"));
    }

    @Test
    void orderAuditIncludingPendingShipmentIsOneRollbackTransaction() throws Exception {
        Method method = ApprovalService.class.getMethod("auditOrder",
                my.hive.domain.approval.model.dto.OrderApprovalAuditRequest.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
