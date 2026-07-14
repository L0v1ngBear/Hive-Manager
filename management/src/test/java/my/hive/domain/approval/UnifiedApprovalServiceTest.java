package my.hive.domain.approval;

import my.hive.api.approval.ApprovalController;
import my.hive.shared.annotation.RequirePermission;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.PermissionCatalogV3;
import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.approval.service.ApprovalService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void canonicalApprovalControllerExposesMergedLeaveSubmitContract() throws Exception {
        Class<?> requestType = Class.forName("my.hive.domain.approval.model.dto.LeaveSubmitRequest");
        Method controllerMethod = ApprovalController.class.getMethod("submitLeave", requestType);

        assertArrayEquals(new String[]{"/leave"}, controllerMethod.getAnnotation(PostMapping.class).value());
        RequirePermission permission = controllerMethod.getAnnotation(RequirePermission.class);
        assertNotNull(permission);
        assertArrayEquals(new String[]{PermissionCatalogV3.CODE_APPROVAL_LEAVE_SUBMIT}, permission.value());

        Method serviceMethod = ApprovalService.class.getMethod("submitLeave", requestType);
        Transactional transactional = serviceMethod.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

    @Test
    void approvalListsAcceptMiniScopeStatusFiltersAndCanonicalLimit() throws Exception {
        assertApprovalListContract("listLeaveApprovals");
        assertApprovalListContract("listFinanceApprovals");
        assertApprovalListContract("listResignationApprovals");
    }

    @Test
    void approvalAuditorPermissionTypeUsesCanonicalQualityOnly() throws Exception {
        ApprovalService service = new ApprovalService();
        Method method = ApprovalService.class.getDeclaredMethod("resolveAuditorPermissionCode", String.class);
        method.setAccessible(true);

        assertEquals(PermissionCatalogV3.CODE_QUALITY_AUDIT, method.invoke(service, "quality"));
        assertThrows(BusinessException.class, () -> {
            try {
                method.invoke(service, "badproduct");
            } catch (java.lang.reflect.InvocationTargetException ex) {
                throw ex.getCause();
            }
        });
        assertThrows(BusinessException.class, () -> {
            try {
                method.invoke(service, "bad_product");
            } catch (java.lang.reflect.InvocationTargetException ex) {
                throw ex.getCause();
            }
        });
    }

    private void assertApprovalListContract(String methodName) throws Exception {
        Method controllerMethod = ApprovalController.class.getMethod(methodName, String.class, Integer.class, Integer.class);
        assertArrayEquals(new String[]{"/" + methodName.replace("list", "").replace("Approvals", "").toLowerCase()},
                controllerMethod.getAnnotation(GetMapping.class).value());

        Method serviceMethod = ApprovalService.class.getMethod(methodName, String.class, Integer.class, Integer.class);
        assertNotNull(serviceMethod);
    }
}
