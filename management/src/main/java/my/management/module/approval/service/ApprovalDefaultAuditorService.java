package my.management.module.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.approval.mapper.ApprovalDefaultAuditorMapper;
import my.management.module.approval.model.dto.ApprovalDefaultAuditorSaveRequest;
import my.management.module.approval.model.entity.ApprovalDefaultAuditor;
import my.management.module.approval.model.vo.ApprovalAuditorOptionVO;
import my.management.module.approval.model.vo.ApprovalDefaultAuditorVO;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ApprovalDefaultAuditorService {

    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_FINANCE = "FINANCE";
    public static final String TYPE_RESIGNATION = "RESIGNATION";
    public static final String TYPE_ORDER = "ORDER";
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DISABLED = 0;

    @Resource
    private ApprovalDefaultAuditorMapper approvalDefaultAuditorMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    public List<ApprovalDefaultAuditorVO> listDefaults() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        return List.of(TYPE_ORDER, TYPE_FINANCE, TYPE_LEAVE, TYPE_RESIGNATION).stream()
                .map(type -> toVO(tenantCode, type))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDefault(ApprovalDefaultAuditorSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String type = normalizeType(request.getApprovalType());
        String permissionCode = permissionCode(type);
        Long auditorId = normalizeAuditorId(request.getAuditorId());
        if (!hasPermission(tenantCode, auditorId, permissionCode)) {
            throw new BusinessException("默认审批人没有对应审批权限，请先分配角色权限");
        }

        ApprovalDefaultAuditor existing = findActiveOrDisabled(tenantCode, type);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            existing = new ApprovalDefaultAuditor();
            existing.setTenantCode(tenantCode);
            existing.setApprovalType(type);
            existing.setCreateTime(now);
        }
        existing.setAuditorId(auditorId);
        existing.setStatus(STATUS_ACTIVE);
        existing.setUpdateTime(now);
        if (existing.getId() == null) {
            approvalDefaultAuditorMapper.insert(existing);
        } else {
            approvalDefaultAuditorMapper.updateById(existing);
        }
    }

    public Long resolveAuditorId(String tenantCode,
                                 String approvalType,
                                 Long applyUserId,
                                 Long specifiedAuditorId,
                                 String permissionCode,
                                 boolean strictSpecified) {
        String normalizedType = normalizeType(approvalType);
        List<Long> permissionAuditorIds = StringUtils.hasText(tenantCode) && StringUtils.hasText(permissionCode)
                ? employeeMapper.selectActiveApproverIdsByPermission(tenantCode, permissionCode)
                : List.of();
        Long specified = normalizeAuditorId(specifiedAuditorId);
        if (specified != null) {
            validateNotSelf(applyUserId, specified);
            if (permissionAuditorIds.contains(specified)) {
                return specified;
            }
            if (strictSpecified) {
                throw new BusinessException("所选审批人没有对应审批权限，请重新选择");
            }
        }

        Long defaultAuditorId = findActiveAuditorId(tenantCode, normalizedType);
        if (defaultAuditorId != null) {
            validateNotSelf(applyUserId, defaultAuditorId);
            if (permissionAuditorIds.contains(defaultAuditorId)) {
                return defaultAuditorId;
            }
        }

        return permissionAuditorIds.stream()
                .filter(id -> id != null && id > 0)
                .filter(id -> applyUserId == null || !applyUserId.equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到可用审批人，请先配置默认审批人或审批角色权限"));
    }

    public List<ApprovalAuditorOptionVO> applyDefaultMark(String approvalType, List<ApprovalAuditorOptionVO> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        Long defaultAuditorId = findActiveAuditorId(TenantPermissionContext.getTenantCode(), normalizeType(approvalType));
        options.forEach(item -> item.setDefaultAuditor(defaultAuditorId != null && defaultAuditorId.equals(item.getId())));
        return options.stream()
                .sorted(Comparator.comparing((ApprovalAuditorOptionVO item) -> !Boolean.TRUE.equals(item.getDefaultAuditor()))
                        .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()))
                .toList();
    }

    public String normalizeType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LEAVE" -> TYPE_LEAVE;
            case "FINANCE" -> TYPE_FINANCE;
            case "RESIGNATION" -> TYPE_RESIGNATION;
            case "ORDER", "ORDER_SALES", "ORDER_PRODUCTION", "SALES", "PRODUCTION" -> TYPE_ORDER;
            default -> throw new BusinessException("审批类型不合法");
        };
    }

    public String permissionCode(String approvalType) {
        return switch (normalizeType(approvalType)) {
            case TYPE_LEAVE -> PermissionCodeEnum.CODE_APPROVAL_LEAVE_AUDIT;
            case TYPE_FINANCE -> PermissionCodeEnum.CODE_APPROVAL_FINANCE_AUDIT;
            case TYPE_RESIGNATION -> PermissionCodeEnum.CODE_APPROVAL_RESIGNATION_AUDIT;
            case TYPE_ORDER -> PermissionCodeEnum.CODE_SALES_ORDER_STATUS;
            default -> throw new BusinessException("审批类型不合法");
        };
    }

    private ApprovalDefaultAuditorVO toVO(String tenantCode, String type) {
        ApprovalDefaultAuditor entity = findActive(tenantCode, type);
        ApprovalDefaultAuditorVO vo = new ApprovalDefaultAuditorVO();
        vo.setApprovalType(type);
        vo.setApprovalTypeText(typeText(type));
        vo.setPermissionCode(permissionCode(type));
        vo.setConfigured(entity != null && entity.getAuditorId() != null);
        if (entity != null) {
            vo.setAuditorId(entity.getAuditorId());
            Employee employee = employeeMapper.selectById(entity.getAuditorId());
            vo.setAuditorName(employee == null ? null : employee.getName());
        }
        return vo;
    }

    private String typeText(String type) {
        return switch (normalizeType(type)) {
            case TYPE_ORDER -> "订单审批";
            case TYPE_FINANCE -> "财务审批";
            case TYPE_LEAVE -> "请假审批";
            case TYPE_RESIGNATION -> "离职审批";
            default -> type;
        };
    }

    private Long findActiveAuditorId(String tenantCode, String type) {
        ApprovalDefaultAuditor entity = findActive(tenantCode, type);
        return entity == null ? null : entity.getAuditorId();
    }

    private ApprovalDefaultAuditor findActive(String tenantCode, String type) {
        if (!StringUtils.hasText(tenantCode)) {
            return null;
        }
        return approvalDefaultAuditorMapper.selectOne(new LambdaQueryWrapper<ApprovalDefaultAuditor>()
                .eq(ApprovalDefaultAuditor::getTenantCode, tenantCode)
                .eq(ApprovalDefaultAuditor::getApprovalType, normalizeType(type))
                .eq(ApprovalDefaultAuditor::getStatus, STATUS_ACTIVE)
                .last("LIMIT 1"));
    }

    private ApprovalDefaultAuditor findActiveOrDisabled(String tenantCode, String type) {
        return approvalDefaultAuditorMapper.selectOne(new LambdaQueryWrapper<ApprovalDefaultAuditor>()
                .eq(ApprovalDefaultAuditor::getTenantCode, tenantCode)
                .eq(ApprovalDefaultAuditor::getApprovalType, normalizeType(type))
                .in(ApprovalDefaultAuditor::getStatus, STATUS_ACTIVE, STATUS_DISABLED)
                .last("LIMIT 1"));
    }

    private boolean hasPermission(String tenantCode, Long auditorId, String permissionCode) {
        if (auditorId == null || auditorId <= 0) {
            return false;
        }
        return employeeMapper.selectActiveApproverIdsByPermission(tenantCode, permissionCode).contains(auditorId);
    }

    private Long normalizeAuditorId(Long auditorId) {
        return auditorId == null || auditorId <= 0 ? null : auditorId;
    }

    private void validateNotSelf(Long applyUserId, Long auditorId) {
        if (applyUserId != null && applyUserId.equals(auditorId)) {
            throw new BusinessException("审批人不能选择申请人本人");
        }
    }
}
