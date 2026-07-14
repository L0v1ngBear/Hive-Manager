package my.hive.domain.approval.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.shared.exception.BusinessException;
import my.hive.domain.approval.mapper.ApprovalAuditorCandidateMapper;
import my.hive.domain.approval.model.entity.ApprovalAuditorCandidate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ApprovalAuditorCandidateService {

    public enum ApprovalDecision {
        LEGACY,
        PENDING,
        APPROVED,
        REJECTED;

        public boolean isCandidateFlow() {
            return this != LEGACY;
        }
    }

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_CLOSED = 2;
    public static final int AUDIT_STATUS_PENDING = 0;
    public static final int AUDIT_STATUS_APPROVED = 1;
    public static final int AUDIT_STATUS_REJECTED = 2;

    @Resource
    private ApprovalAuditorCandidateMapper approvalAuditorCandidateMapper;

    public List<Long> findActiveAuditorIds(String tenantCode, String approvalType, String approvalCode) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType) || !StringUtils.hasText(approvalCode)) {
            return List.of();
        }
        List<ApprovalAuditorCandidate> rows = approvalAuditorCandidateMapper.selectList(
                new LambdaQueryWrapper<ApprovalAuditorCandidate>()
                        .eq(ApprovalAuditorCandidate::getApprovalType, approvalType)
                        .eq(ApprovalAuditorCandidate::getApprovalCode, approvalCode)
                        .eq(ApprovalAuditorCandidate::getStatus, STATUS_ACTIVE)
                        .orderByAsc(ApprovalAuditorCandidate::getId));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Long> auditorIds = new ArrayList<>();
        for (ApprovalAuditorCandidate row : rows) {
            if (row.getAuditorId() != null && row.getAuditorId() > 0 && !auditorIds.contains(row.getAuditorId())) {
                auditorIds.add(row.getAuditorId());
            }
        }
        return auditorIds;
    }

    public List<Long> findPendingAuditorIds(String tenantCode, String approvalType, String approvalCode) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType) || !StringUtils.hasText(approvalCode)) {
            return List.of();
        }
        List<ApprovalAuditorCandidate> rows = approvalAuditorCandidateMapper.selectList(
                new LambdaQueryWrapper<ApprovalAuditorCandidate>()
                        .eq(ApprovalAuditorCandidate::getApprovalType, approvalType)
                        .eq(ApprovalAuditorCandidate::getApprovalCode, approvalCode)
                        .eq(ApprovalAuditorCandidate::getStatus, STATUS_ACTIVE)
                        .eq(ApprovalAuditorCandidate::getAuditStatus, AUDIT_STATUS_PENDING)
                        .orderByAsc(ApprovalAuditorCandidate::getId));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Long> auditorIds = new ArrayList<>();
        for (ApprovalAuditorCandidate row : rows) {
            if (row.getAuditorId() != null && row.getAuditorId() > 0 && !auditorIds.contains(row.getAuditorId())) {
                auditorIds.add(row.getAuditorId());
            }
        }
        return auditorIds;
    }

    public boolean isPendingAuditor(String tenantCode, String approvalType, String approvalCode, Long auditorId) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType)
                || !StringUtils.hasText(approvalCode) || auditorId == null || auditorId <= 0) {
            return false;
        }
        Long count = approvalAuditorCandidateMapper.selectCount(new LambdaQueryWrapper<ApprovalAuditorCandidate>()
                .eq(ApprovalAuditorCandidate::getApprovalType, approvalType)
                .eq(ApprovalAuditorCandidate::getApprovalCode, approvalCode)
                .eq(ApprovalAuditorCandidate::getAuditorId, auditorId)
                .eq(ApprovalAuditorCandidate::getStatus, STATUS_ACTIVE)
                .eq(ApprovalAuditorCandidate::getAuditStatus, AUDIT_STATUS_PENDING));
        return count != null && count > 0;
    }

    public long countPendingAudits(String tenantCode, String approvalType, Long auditorId) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType) || auditorId == null || auditorId <= 0) {
            return 0L;
        }
        Long count = approvalAuditorCandidateMapper.selectCount(new LambdaQueryWrapper<ApprovalAuditorCandidate>()
                .eq(ApprovalAuditorCandidate::getApprovalType, approvalType)
                .eq(ApprovalAuditorCandidate::getAuditorId, auditorId)
                .eq(ApprovalAuditorCandidate::getStatus, STATUS_ACTIVE)
                .eq(ApprovalAuditorCandidate::getAuditStatus, AUDIT_STATUS_PENDING));
        return count == null ? 0L : count;
    }

    public List<String> findPendingApprovalCodes(String tenantCode, String approvalType, Long auditorId) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType) || auditorId == null || auditorId <= 0) {
            return List.of();
        }
        List<ApprovalAuditorCandidate> rows = approvalAuditorCandidateMapper.selectList(
                new LambdaQueryWrapper<ApprovalAuditorCandidate>()
                        .eq(ApprovalAuditorCandidate::getApprovalType, approvalType)
                        .eq(ApprovalAuditorCandidate::getAuditorId, auditorId)
                        .eq(ApprovalAuditorCandidate::getStatus, STATUS_ACTIVE)
                        .eq(ApprovalAuditorCandidate::getAuditStatus, AUDIT_STATUS_PENDING)
                        .orderByDesc(ApprovalAuditorCandidate::getId));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (ApprovalAuditorCandidate row : rows) {
            if (StringUtils.hasText(row.getApprovalCode())) {
                codes.add(row.getApprovalCode());
            }
        }
        return new ArrayList<>(codes);
    }

    public boolean hasPendingAuditors(String tenantCode, String approvalType, String approvalCode) {
        return !findPendingAuditorIds(tenantCode, approvalType, approvalCode).isEmpty();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ApprovalDecision recordDecision(String tenantCode,
                                           String approvalType,
                                           String approvalCode,
                                           Long auditorId,
                                           boolean approved,
                                           String comment) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType)
                || !StringUtils.hasText(approvalCode) || auditorId == null || auditorId <= 0) {
            throw new BusinessException("approval decision parameters are invalid");
        }
        List<ApprovalAuditorCandidate> history = approvalAuditorCandidateMapper.selectApprovalForUpdate(
                tenantCode, approvalType, approvalCode);
        if (history == null || history.isEmpty()) {
            return ApprovalDecision.LEGACY;
        }
        boolean pendingCandidate = history.stream().anyMatch(candidate ->
                STATUS_ACTIVE == valueOrDefault(candidate.getStatus())
                        && AUDIT_STATUS_PENDING == valueOrDefault(candidate.getAuditStatus())
                        && auditorId.equals(candidate.getAuditorId()));
        if (!pendingCandidate) {
            throw new BusinessException("approval has already been processed or current user is not an auditor");
        }

        LocalDateTime auditTime = LocalDateTime.now();
        int updatedRows = approvalAuditorCandidateMapper.updatePendingDecision(
                tenantCode,
                approvalType,
                approvalCode,
                auditorId,
                approved ? AUDIT_STATUS_APPROVED : AUDIT_STATUS_REJECTED,
                comment,
                auditTime);
        if (updatedRows != 1) {
            throw new BusinessException("approval has already been processed or current user is not an auditor");
        }

        List<ApprovalAuditorCandidate> current = approvalAuditorCandidateMapper.selectApprovalForUpdate(
                tenantCode, approvalType, approvalCode);
        List<ApprovalAuditorCandidate> active = current == null ? List.of() : current.stream()
                .filter(candidate -> STATUS_ACTIVE == valueOrDefault(candidate.getStatus()))
                .toList();
        if (active.isEmpty()) {
            throw new BusinessException("approval instance is no longer active");
        }
        if (active.stream().anyMatch(candidate -> AUDIT_STATUS_REJECTED == valueOrDefault(candidate.getAuditStatus()))) {
            return ApprovalDecision.REJECTED;
        }
        if (active.stream().anyMatch(candidate -> AUDIT_STATUS_PENDING == valueOrDefault(candidate.getAuditStatus()))) {
            return ApprovalDecision.PENDING;
        }
        if (active.stream().allMatch(candidate -> AUDIT_STATUS_APPROVED == valueOrDefault(candidate.getAuditStatus()))) {
            return ApprovalDecision.APPROVED;
        }
        throw new BusinessException("approval candidate state is invalid");
    }

    public boolean markAuditorDecision(String tenantCode,
                                       String approvalType,
                                       String approvalCode,
                                       Long auditorId,
                                       boolean approved,
                                       String comment) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType)
                || !StringUtils.hasText(approvalCode) || auditorId == null || auditorId <= 0) {
            return false;
        }
        int rows = approvalAuditorCandidateMapper.update(null, new LambdaUpdateWrapper<ApprovalAuditorCandidate>()
                .eq(ApprovalAuditorCandidate::getApprovalType, approvalType)
                .eq(ApprovalAuditorCandidate::getApprovalCode, approvalCode)
                .eq(ApprovalAuditorCandidate::getAuditorId, auditorId)
                .eq(ApprovalAuditorCandidate::getStatus, STATUS_ACTIVE)
                .eq(ApprovalAuditorCandidate::getAuditStatus, AUDIT_STATUS_PENDING)
                .set(ApprovalAuditorCandidate::getAuditStatus, approved ? AUDIT_STATUS_APPROVED : AUDIT_STATUS_REJECTED)
                .set(ApprovalAuditorCandidate::getAuditComment, comment)
                .set(ApprovalAuditorCandidate::getAuditTime, LocalDateTime.now())
                .set(ApprovalAuditorCandidate::getUpdateTime, LocalDateTime.now()));
        return rows > 0;
    }

    private int valueOrDefault(Integer value) {
        return value == null ? -1 : value;
    }

    public void replaceActiveCandidates(String tenantCode, String approvalType, String approvalCode, List<Long> auditorIds) {
        closeActiveCandidates(tenantCode, approvalType, approvalCode);
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType)
                || !StringUtils.hasText(approvalCode) || auditorIds == null || auditorIds.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(auditorIds);
        LocalDateTime now = LocalDateTime.now();
        for (Long auditorId : uniqueIds) {
            if (auditorId == null || auditorId <= 0) {
                continue;
            }
            ApprovalAuditorCandidate candidate = new ApprovalAuditorCandidate();
            candidate.setTenantCode(tenantCode);
            candidate.setApprovalType(approvalType);
            candidate.setApprovalCode(approvalCode);
            candidate.setAuditorId(auditorId);
            candidate.setStatus(STATUS_ACTIVE);
            candidate.setAuditStatus(AUDIT_STATUS_PENDING);
            candidate.setCreateTime(now);
            candidate.setUpdateTime(now);
            approvalAuditorCandidateMapper.insert(candidate);
        }
    }

    public void closeActiveCandidates(String tenantCode, String approvalType, String approvalCode) {
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(approvalType) || !StringUtils.hasText(approvalCode)) {
            return;
        }
        approvalAuditorCandidateMapper.update(null, new LambdaUpdateWrapper<ApprovalAuditorCandidate>()
                .eq(ApprovalAuditorCandidate::getApprovalType, approvalType)
                .eq(ApprovalAuditorCandidate::getApprovalCode, approvalCode)
                .eq(ApprovalAuditorCandidate::getStatus, STATUS_ACTIVE)
                .set(ApprovalAuditorCandidate::getStatus, STATUS_CLOSED)
                .set(ApprovalAuditorCandidate::getUpdateTime, LocalDateTime.now()));
    }
}
