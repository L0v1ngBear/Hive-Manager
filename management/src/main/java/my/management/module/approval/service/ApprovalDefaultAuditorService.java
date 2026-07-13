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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class ApprovalDefaultAuditorService {

    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_FINANCE = "FINANCE";
    public static final String TYPE_RESIGNATION = "RESIGNATION";
    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_QUALITY = "QUALITY";
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DISABLED = 0;

    @Resource
    private ApprovalDefaultAuditorMapper approvalDefaultAuditorMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    public List<ApprovalDefaultAuditorVO> listDefaults() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        return List.of(TYPE_ORDER, TYPE_QUALITY, TYPE_FINANCE, TYPE_LEAVE, TYPE_RESIGNATION).stream()
                .map(type -> toVO(tenantCode, type))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDefault(ApprovalDefaultAuditorSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String type = normalizeType(request.getApprovalType());
        String permissionCode = permissionCode(type);
        List<Long> auditorIds = normalizeAuditorIds(request.getAuditorIds(), request.getAuditorId());
        if (auditorIds.isEmpty()) {
            throw new BusinessException("默认审批人不能为空");
        }
        for (Long auditorId : auditorIds) {
            if (!hasPermission(tenantCode, auditorId, permissionCode)) {
                throw new BusinessException("默认审批人没有对应审批权限，请先分配角色权限");
            }
        }

        ApprovalDefaultAuditor existing = findActiveOrDisabled(tenantCode, type);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            existing = new ApprovalDefaultAuditor();
            existing.setTenantCode(tenantCode);
            existing.setApprovalType(type);
            existing.setCreateTime(now);
        }
        existing.setAuditorId(auditorIds.get(0));
        existing.setAuditorIds(auditorIds.size() > 1 ? joinAuditorIds(auditorIds) : null);
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
        return resolveAuditorIds(tenantCode, approvalType, applyUserId, specifiedAuditorId, null, permissionCode, strictSpecified)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到可用审批人，请先配置默认审批人或审批角色权限"));
    }

    public List<Long> resolveAuditorIds(String tenantCode,
                                        String approvalType,
                                        Long applyUserId,
                                        Long specifiedAuditorId,
                                        List<Long> specifiedAuditorIds,
                                        String permissionCode,
                                        boolean strictSpecified) {
        String normalizedType = normalizeType(approvalType);
        List<Long> permissionAuditorIds = StringUtils.hasText(tenantCode) && StringUtils.hasText(permissionCode)
                ? employeeMapper.selectActiveApproverIdsByPermission(tenantCode, permissionCode)
                : List.of();
        List<Long> specifiedIds = normalizeAuditorIds(specifiedAuditorIds, null);
        if (!specifiedIds.isEmpty()) {
            validateAuditorIds(applyUserId, specifiedIds, permissionAuditorIds, strictSpecified);
            return specifiedIds;
        }

        Long specified = normalizeAuditorId(specifiedAuditorId);
        if (specified != null) {
            validateNotSelf(applyUserId, specified);
            if (permissionAuditorIds.contains(specified)) {
                return List.of(specified);
            }
            if (strictSpecified) {
                throw new BusinessException("所选审批人没有对应审批权限，请重新选择");
            }
        }

        List<Long> defaultAuditorIds = findActiveAuditorIds(tenantCode, normalizedType);
        if (!defaultAuditorIds.isEmpty()) {
            validateAuditorIds(applyUserId, defaultAuditorIds, permissionAuditorIds, false);
            return defaultAuditorIds;
        }

        Long fallbackAuditorId = permissionAuditorIds.stream()
                .filter(id -> id != null && id > 0)
                .filter(id -> applyUserId == null || !applyUserId.equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到可用审批人，请先配置默认审批人或审批角色权限"));
        return List.of(fallbackAuditorId);
    }

    public List<ApprovalAuditorOptionVO> applyDefaultMark(String approvalType, List<ApprovalAuditorOptionVO> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        List<Long> defaultAuditorIds = findActiveAuditorIds(TenantPermissionContext.getTenantCode(), normalizeType(approvalType));
        options.forEach(item -> item.setDefaultAuditor(item.getId() != null && defaultAuditorIds.contains(item.getId())));
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
            case "QUALITY", "BADPRODUCT", "BAD_PRODUCT" -> TYPE_QUALITY;
            default -> throw new BusinessException("审批类型不合法");
        };
    }

    public String permissionCode(String approvalType) {
        return switch (normalizeType(approvalType)) {
            case TYPE_LEAVE -> PermissionCodeEnum.CODE_APPROVAL_LEAVE_AUDIT;
            case TYPE_FINANCE -> PermissionCodeEnum.CODE_APPROVAL_FINANCE_AUDIT;
            case TYPE_RESIGNATION -> PermissionCodeEnum.CODE_APPROVAL_RESIGNATION_AUDIT;
            case TYPE_ORDER -> PermissionCodeEnum.CODE_APPROVAL_ORDER_AUDIT;
            case TYPE_QUALITY -> PermissionCodeEnum.CODE_BADPRODUCT_PROCESS;
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
            List<Long> auditorIds = resolveEntityAuditorIds(entity);
            vo.setAuditorIds(auditorIds);
            vo.setAuditorName(resolveAuditorNames(auditorIds));
        }
        return vo;
    }

    private String typeText(String type) {
        return switch (normalizeType(type)) {
            case TYPE_ORDER -> "订单审批";
            case TYPE_FINANCE -> "财务审批";
            case TYPE_LEAVE -> "请假审批";
            case TYPE_RESIGNATION -> "离职审批";
            case TYPE_QUALITY -> "质量审核";
            default -> type;
        };
    }

    private Long findActiveAuditorId(String tenantCode, String type) {
        ApprovalDefaultAuditor entity = findActive(tenantCode, type);
        return entity == null ? null : entity.getAuditorId();
    }

    private List<Long> findActiveAuditorIds(String tenantCode, String type) {
        ApprovalDefaultAuditor entity = findActive(tenantCode, type);
        return resolveEntityAuditorIds(entity);
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

    private List<Long> normalizeAuditorIds(List<Long> auditorIds, Long fallbackAuditorId) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (auditorIds != null) {
            for (Long auditorId : auditorIds) {
                Long normalized = normalizeAuditorId(auditorId);
                if (normalized != null) {
                    ids.add(normalized);
                }
            }
        }
        Long fallback = normalizeAuditorId(fallbackAuditorId);
        if (ids.isEmpty() && fallback != null) {
            ids.add(fallback);
        }
        return new ArrayList<>(ids);
    }

    private List<Long> resolveEntityAuditorIds(ApprovalDefaultAuditor entity) {
        if (entity == null) {
            return List.of();
        }
        List<Long> ids = parseAuditorIds(entity.getAuditorIds());
        if (ids.isEmpty() && entity.getAuditorId() != null && entity.getAuditorId() > 0) {
            return List.of(entity.getAuditorId());
        }
        return ids;
    }

    private List<Long> parseAuditorIds(String auditorIds) {
        if (!StringUtils.hasText(auditorIds)) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String raw : auditorIds.split(",")) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            try {
                long value = Long.parseLong(raw.trim());
                if (value > 0) {
                    ids.add(value);
                }
            } catch (NumberFormatException ignored) {
                // 历史脏数据不应影响审批配置页面展示。
            }
        }
        return new ArrayList<>(ids);
    }

    private String joinAuditorIds(List<Long> auditorIds) {
        return auditorIds == null || auditorIds.isEmpty()
                ? null
                : String.join(",", auditorIds.stream().map(String::valueOf).toList());
    }

    private String resolveAuditorNames(List<Long> auditorIds) {
        if (auditorIds == null || auditorIds.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Long auditorId : auditorIds) {
            Employee employee = employeeMapper.selectById(auditorId);
            if (employee != null && StringUtils.hasText(employee.getName())) {
                names.add(employee.getName());
            }
        }
        return names.isEmpty() ? null : String.join("、", names);
    }

    private void validateAuditorIds(Long applyUserId,
                                    List<Long> auditorIds,
                                    List<Long> permissionAuditorIds,
                                    boolean strictSpecified) {
        for (Long auditorId : auditorIds) {
            validateNotSelf(applyUserId, auditorId);
            if (!permissionAuditorIds.contains(auditorId) && strictSpecified) {
                throw new BusinessException("所选审批人没有对应审批权限，请重新选择");
            }
            if (!permissionAuditorIds.contains(auditorId) && !strictSpecified) {
                throw new BusinessException("默认审批人没有对应审批权限，请先分配角色权限");
            }
        }
    }

    private void validateNotSelf(Long applyUserId, Long auditorId) {
        if (applyUserId != null && applyUserId.equals(auditorId)) {
            throw new BusinessException("审批人不能选择申请人本人");
        }
    }
}
