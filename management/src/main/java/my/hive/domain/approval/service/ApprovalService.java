package my.hive.domain.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.security.InternalUploadUrlValidator;
import my.hive.shared.utils.CodeGeneratorUtil;
import my.hive.domain.approval.mapper.FinanceApprovalMapper;
import my.hive.domain.approval.mapper.LeaveMapper;
import my.hive.domain.approval.mapper.ResignationApprovalMapper;
import my.hive.domain.approval.model.dto.FinanceAuditRequest;
import my.hive.domain.approval.model.dto.FinanceSubmitRequest;
import my.hive.domain.approval.model.dto.LeaveAuditRequest;
import my.hive.domain.approval.model.dto.LeaveSubmitRequest;
import my.hive.domain.approval.model.dto.OrderApprovalAuditRequest;
import my.hive.domain.approval.model.dto.QualityAuditRequest;
import my.hive.domain.approval.model.dto.ResignationAuditRequest;
import my.hive.domain.approval.model.dto.ResignationSubmitRequest;
import my.hive.domain.approval.model.entity.FinanceApproval;
import my.hive.domain.approval.model.entity.ResignationApproval;
import my.hive.domain.approval.model.entity.UserLeave;
import my.hive.domain.approval.model.enums.ApprovalActionEnum;
import my.hive.domain.approval.model.enums.ApprovalStatusEnum;
import my.hive.domain.approval.model.enums.LeaveTypeEnum;
import my.hive.domain.approval.model.vo.ApprovalSummaryVO;
import my.hive.domain.approval.model.vo.ApprovalAuditorOptionVO;
import my.hive.domain.approval.model.vo.FinanceApprovalVO;
import my.hive.domain.approval.model.vo.LeaveApprovalListVO;
import my.hive.domain.approval.model.vo.LeaveDetailVO;
import my.hive.domain.approval.model.vo.OrderApprovalVO;
import my.hive.domain.approval.model.vo.QualityApprovalVO;
import my.hive.domain.approval.model.vo.ResignationApprovalVO;
import my.hive.domain.attendance.mapper.AttendanceRecordMapper;
import my.hive.domain.attendance.mapper.TenantAttendanceRuleManageMapper;
import my.hive.domain.attendance.model.entity.AttendanceRecord;
import my.hive.domain.attendance.model.entity.TenantAttendanceRule;
import my.hive.domain.attendance.model.enums.AttendancePunchStatusEnum;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.employee.service.EmployeeService;
import my.hive.domain.quality.mapper.BadProductMapper;
import my.hive.domain.quality.model.entity.BadProductRecord;
import my.hive.domain.quality.service.QualityService;
import my.hive.domain.order.mapper.ProductionOrderMapper;
import my.hive.domain.order.mapper.SalesOrderMapper;
import my.hive.domain.order.model.dto.ProductionOrderUpdateRequest;
import my.hive.domain.order.model.dto.SalesOrderUpdateRequest;
import my.hive.domain.order.model.entity.ProductionOrder;
import my.hive.domain.order.model.entity.ProductionOrderStatusLog;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.entity.SalesOrderStatusLog;
import my.hive.domain.order.model.enums.OrderCategoryEnum;
import my.hive.domain.order.service.OrderService;
import my.hive.shared.permission.PermissionCatalogV3;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
/**
 * ApprovalService 属于管理端后端审批模块，实现核心业务编排与规则逻辑。
 */
@Service
public class ApprovalService {

    private static final String ORDER_TYPE_SALES = "sales";
    private static final String ORDER_TYPE_PRODUCTION = "production";
    private static final String ORDER_STATUS_PENDING_CONFIRM = "pending_confirm";
    private static final String ORDER_STATUS_PENDING_PAY = "pending_pay";
    private static final String ORDER_STATUS_PENDING_MATERIAL = "pending_material";
    private static final String ORDER_STATUS_PENDING_SHIP = "pending_ship";
    private static final String ORDER_STATUS_SHIPPED = "shipped";
    private static final String ORDER_STATUS_PENDING_CANCEL = "pending_cancel";
    private static final String ORDER_STATUS_CANCELLED = "cancelled";
    private static final String SALES_APPROVED_NEXT_STATUS = "pending_pay";
    private static final String APPROVAL_TYPE_LEAVE = "LEAVE";
    private static final String APPROVAL_TYPE_FINANCE = "FINANCE";
    private static final String APPROVAL_TYPE_RESIGNATION = "RESIGNATION";
    private static final String APPROVAL_TYPE_ORDER = "ORDER";
    private static final String APPROVAL_TYPE_QUALITY = "QUALITY";
    private static final String BAD_PRODUCT_STATUS_PENDING_AUDIT = "pending_audit";
    private static final String BAD_PRODUCT_STATUS_PROCESSED = "processed";
    private static final int MAX_PARALLEL_APPROVERS = 30;
    private static final int DEFAULT_APPROVAL_LIST_LIMIT = 100;
    private static final int MAX_APPROVAL_LIST_LIMIT = 500;
    private static final int DEFAULT_AUDITOR_OPTION_LIMIT = 20;
    private static final int MAX_AUDITOR_OPTION_LIMIT = 50;
    private static final long MAX_LEAVE_SYNC_DAYS = 366L;

    @Resource
    private LeaveMapper leaveMapper;

    @Resource
    private FinanceApprovalMapper financeApprovalMapper;

    @Resource
    private ResignationApprovalMapper resignationApprovalMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private AttendanceRecordMapper attendanceRecordMapper;

    @Resource
    private TenantAttendanceRuleManageMapper tenantAttendanceRuleManageMapper;

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private ProductionOrderMapper productionOrderMapper;

    @Resource
    private OrderService orderService;

    @Resource
    private BadProductMapper badProductMapper;

    @Resource
    private QualityService qualityService;

    @Resource
    private ApprovalAuditorCandidateService approvalAuditorCandidateService;

    @Resource
    private ApprovalDefaultAuditorService approvalDefaultAuditorService;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public ApprovalSummaryVO getSummary() {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();

        LambdaQueryWrapper<UserLeave> leavePendingWrapper = new LambdaQueryWrapper<UserLeave>()
                .eq(UserLeave::getStatus, ApprovalStatusEnum.PENDING.getCode());
        appendLeaveAuditorFilter(leavePendingWrapper, userId);
        long leavePending = safeCount(leaveMapper.selectCount(leavePendingWrapper));

        LambdaQueryWrapper<FinanceApproval> financePendingWrapper = new LambdaQueryWrapper<FinanceApproval>()
                .eq(FinanceApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
        appendFinanceAuditorFilter(financePendingWrapper, userId);
        long financePending = safeCount(financeApprovalMapper.selectCount(financePendingWrapper));

        LambdaQueryWrapper<ResignationApproval> resignationPendingWrapper = new LambdaQueryWrapper<ResignationApproval>()
                .eq(ResignationApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
        appendResignationAuditorFilter(resignationPendingWrapper, userId);
        long resignationPending = safeCount(resignationApprovalMapper.selectCount(resignationPendingWrapper));
        long orderPending = countPendingOrderApprovals(tenantCode, userId);
        long qualityPending = approvalAuditorCandidateService.countPendingAudits(tenantCode, APPROVAL_TYPE_QUALITY, userId);

        ApprovalSummaryVO vo = new ApprovalSummaryVO();
        vo.setLeavePending(leavePending);
        vo.setFinancePending(financePending);
        vo.setResignationPending(resignationPending);
        vo.setOrderPending(orderPending);
        vo.setQualityPending(qualityPending);
        vo.setTotalPending(leavePending + financePending + resignationPending + orderPending + qualityPending);
        return vo;
    }

    public List<ApprovalAuditorOptionVO> listAuditorOptions(String type, String keyword, Integer limit) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String permissionCode = resolveAuditorPermissionCode(type);
        if (!StringUtils.hasText(tenantCode) || !StringUtils.hasText(permissionCode)) {
            return List.of();
        }
        String normalizedType = approvalDefaultAuditorService.normalizeType(type);
        return approvalDefaultAuditorService.applyDefaultMark(normalizedType, employeeMapper.selectActiveApproverOptionsByPermission(
                tenantCode,
                permissionCode,
                trimToNull(keyword),
                safeAuditorOptionLimit(limit)
        ));
    }

    public List<LeaveApprovalListVO> listLeaveApprovals(String scope, Integer status, Integer limit) {
        String normalizedScope = requireApprovalListScope(
                scope,
                PermissionCatalogV3.CODE_APPROVAL_LEAVE_SUBMIT,
                PermissionCatalogV3.CODE_APPROVAL_LEAVE_LIST,
                PermissionCatalogV3.CODE_APPROVAL_LEAVE_AUDIT,
                "请假");
        Long userId = TenantPermissionContext.getUserId();
        LambdaQueryWrapper<UserLeave> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(UserLeave::getStatus, status);
        }
        applyLeaveScope(wrapper, normalizedScope, userId);
        wrapper.orderByDesc(UserLeave::getCreateTime);
        wrapper.last("LIMIT " + safeApprovalListLimit(limit));
        return leaveMapper.selectList(wrapper).stream().map(this::toLeaveListVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public String submitLeave(LeaveSubmitRequest request) {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException("提交失败：请假开始时间必须早于结束时间");
        }
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("提交失败：不能提交过去的请假申请");
        }
        boolean hasOverlap = leaveMapper.exists(new LambdaQueryWrapper<UserLeave>()
                .eq(UserLeave::getApplyUserId, userId)
                .ne(UserLeave::getStatus, ApprovalStatusEnum.REJECTED.getCode())
                .and(wrapper -> wrapper
                        .lt(UserLeave::getStartTime, request.getEndTime())
                        .gt(UserLeave::getEndTime, request.getStartTime())));
        if (hasOverlap) {
            throw new BusinessException("提交失败：该时间段已有待审批或已通过的请假单");
        }

        UserLeave approval = new UserLeave();
        approval.setLeaveCode(codeGeneratorUtil.generateCode("LV", 4));
        approval.setTenantCode(tenantCode);
        approval.setApplyUserId(userId);
        approval.setLeaveType(request.getLeaveType());
        approval.setStartTime(request.getStartTime());
        approval.setEndTime(request.getEndTime());
        approval.setReason(request.getReason().trim());
        approval.setStatus(ApprovalStatusEnum.PENDING.getCode());
        assignLeaveAuditors(approval, request.getAuditorId(), request.getAuditorIds(), true);
        leaveMapper.insert(approval);
        return approval.getLeaveCode();
    }

    public LeaveDetailVO getLeaveDetail(String leaveCode) {
        UserLeave userLeave = getLeaveByCode(leaveCode);
        LeaveDetailVO vo = new LeaveDetailVO();
        BeanUtils.copyProperties(userLeave, vo);
        Employee applyUser = employeeMapper.selectById(userLeave.getApplyUserId());
        vo.setApplyUserName(applyUser == null ? "未知员工" : applyUser.getName());
        vo.setAuditorName(resolveAuditorNames(userLeave.getAuditorId(), userLeave.getAuditorIds()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditLeave(LeaveAuditRequest request) {
        Long currentUserId = TenantPermissionContext.getUserId();
        UserLeave userLeave = getLeaveByCode(request.getLeaveCode());
        if (!canCurrentUserAudit(currentUserId, userLeave.getAuditorId(), userLeave.getAuditorIds())) {
            throw new BusinessException("您不是请假单的当前审批人");
        }
        if (!ApprovalStatusEnum.isPending(userLeave.getStatus())) {
            throw new BusinessException("该请假单已处理，请勿重复审批");
        }
        String auditComment = trimToNull(request.getComment());
        boolean approve = ApprovalActionEnum.isApprove(request.getAction());
        ApprovalAuditorCandidateService.ApprovalDecision decision = recordCandidateDecision(
                userLeave.getTenantCode(), APPROVAL_TYPE_LEAVE, userLeave.getLeaveCode(), currentUserId, approve, auditComment);
        if (decision.isCandidateFlow()) {
            userLeave.setAuditComment(auditComment);
            if (!approve) {
                userLeave.setStatus(ApprovalStatusEnum.REJECTED.getCode());
                approvalAuditorCandidateService.closeActiveCandidates(
                        userLeave.getTenantCode(), APPROVAL_TYPE_LEAVE, userLeave.getLeaveCode());
                leaveMapper.updateById(userLeave);
                return;
            }
            if (decision == ApprovalAuditorCandidateService.ApprovalDecision.PENDING) {
                leaveMapper.updateById(userLeave);
                return;
            }
            userLeave.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            syncLeaveToAttendance(userLeave);
            approvalAuditorCandidateService.closeActiveCandidates(
                    userLeave.getTenantCode(), APPROVAL_TYPE_LEAVE, userLeave.getLeaveCode());
            leaveMapper.updateById(userLeave);
            return;
        }
        userLeave.setAuditComment(auditComment);
        if (approve) {
            Long nextManagerId = getManagerId(currentUserId);
            Integer roleLevel = getRoleLevel(currentUserId);
            if ((roleLevel != null && roleLevel >= 2) || nextManagerId == null) {
                userLeave.setStatus(ApprovalStatusEnum.APPROVED.getCode());
                syncLeaveToAttendance(userLeave);
            } else {
                assignLeaveAuditors(userLeave, nextManagerId);
            }
        } else {
            userLeave.setStatus(ApprovalStatusEnum.REJECTED.getCode());
        }
        if (!ApprovalStatusEnum.isPending(userLeave.getStatus())) {
            approvalAuditorCandidateService.closeActiveCandidates(
                    userLeave.getTenantCode(), APPROVAL_TYPE_LEAVE, userLeave.getLeaveCode());
        }
        leaveMapper.updateById(userLeave);
    }

    /**
     * 请假审批最终通过后，同步写入考勤记录。
     * 这样无论审批是在小程序还是管理端完成，考勤页和每日统计都能识别该员工当天处于请假状态。
     */
    private void syncLeaveToAttendance(UserLeave leave) {
        if (leave == null || leave.getStartTime() == null || leave.getEndTime() == null) {
            throw new BusinessException("请假时间不完整，无法同步考勤");
        }
        LocalDate currentDate = leave.getStartTime().toLocalDate();
        LocalDate endDate = leave.getEndTime().toLocalDate();
        long syncDays = ChronoUnit.DAYS.between(currentDate, endDate) + 1;
        if (syncDays <= 0) {
            throw new BusinessException("请假结束时间不能早于开始时间");
        }
        if (syncDays > MAX_LEAVE_SYNC_DAYS) {
            throw new BusinessException("请假跨度不能超过 " + MAX_LEAVE_SYNC_DAYS + " 天，请拆分后提交");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        TenantAttendanceRule rule = tenantAttendanceRuleManageMapper.selectByTenantCode(leave.getTenantCode());

        while (!currentDate.isAfter(endDate)) {
            boolean coverSignIn = isLeaveOverlapTimeRange(currentDate, rule == null ? null : rule.getWorkStartTime(),
                    rule == null ? null : rule.getWorkEndTime(), leave);
            boolean coverSignOut = isLeaveOverlapTimeRange(currentDate, rule == null ? null : rule.getOffWorkStartTime(),
                    rule == null ? null : rule.getOffWorkEndTime(), leave);
            if (!coverSignIn && !coverSignOut) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            String punchId = currentDate.format(formatter) + "_" + leave.getApplyUserId();
            AttendanceRecord record = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getPunchId, punchId));

            if (record == null) {
                record = new AttendanceRecord();
                record.setPunchId(punchId);
                record.setTenantCode(leave.getTenantCode());
                record.setUserId(leave.getApplyUserId());
                if (coverSignIn) {
                    record.setSignInStatus(AttendancePunchStatusEnum.LEAVE.getCode());
                }
                if (coverSignOut) {
                    record.setSignOutStatus(AttendancePunchStatusEnum.LEAVE.getCode());
                }
                attendanceRecordMapper.insert(record);
            } else if (fillLeaveStatus(record, coverSignIn, coverSignOut)) {
                attendanceRecordMapper.updateById(record);
            }
            currentDate = currentDate.plusDays(1);
        }
    }

    /**
     * 只覆盖空状态或异常状态，保留已经正常打卡/加班的真实记录。
     */
    private boolean fillLeaveStatus(AttendanceRecord record, boolean coverSignIn, boolean coverSignOut) {
        boolean changed = false;
        if (coverSignIn && AttendancePunchStatusEnum.canBeCoveredByLeaveForSignIn(record.getSignInStatus())) {
            record.setSignInStatus(AttendancePunchStatusEnum.LEAVE.getCode());
            changed = true;
        }
        if (coverSignOut && AttendancePunchStatusEnum.canBeCoveredByLeaveForSignOut(record.getSignOutStatus())) {
            record.setSignOutStatus(AttendancePunchStatusEnum.LEAVE.getCode());
            changed = true;
        }
        return changed;
    }

    /**
     * 判断请假时间是否覆盖某个打卡时间段。
     * 未配置规则时按全天覆盖处理，避免老租户没有规则导致请假无法同步。
     */
    private boolean isLeaveOverlapTimeRange(LocalDate date, LocalTime rangeStart, LocalTime rangeEnd, UserLeave leave) {
        if (rangeStart == null || rangeEnd == null) {
            return true;
        }
        LocalDateTime segmentStart = LocalDateTime.of(date, rangeStart);
        LocalDateTime segmentEnd = LocalDateTime.of(date, rangeEnd);
        return leave.getStartTime().isBefore(segmentEnd) && leave.getEndTime().isAfter(segmentStart);
    }

    public List<FinanceApprovalVO> listFinanceApprovals(String scope, Integer status, Integer limit) {
        String normalizedScope = requireApprovalListScope(
                scope,
                PermissionCatalogV3.CODE_APPROVAL_FINANCE_SUBMIT,
                PermissionCatalogV3.CODE_APPROVAL_FINANCE_LIST,
                PermissionCatalogV3.CODE_APPROVAL_FINANCE_AUDIT,
                "财务");
        Long userId = TenantPermissionContext.getUserId();
        LambdaQueryWrapper<FinanceApproval> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(FinanceApproval::getStatus, status);
        }
        applyFinanceScope(wrapper, normalizedScope, userId);
        wrapper.orderByDesc(FinanceApproval::getCreateTime);
        wrapper.last("LIMIT " + safeApprovalListLimit(limit));
        return financeApprovalMapper.selectList(wrapper).stream().map(this::toFinanceVO).toList();
    }

    public FinanceApprovalVO getFinanceDetail(String approvalCode) {
        return toFinanceVO(getFinanceByCode(approvalCode));
    }

    public List<ResignationApprovalVO> listResignationApprovals(String scope, Integer status, Integer limit) {
        String normalizedScope = requireApprovalListScope(
                scope,
                PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_SUBMIT,
                PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_LIST,
                PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_AUDIT,
                "离职");
        Long userId = TenantPermissionContext.getUserId();
        LambdaQueryWrapper<ResignationApproval> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ResignationApproval::getStatus, status);
        }
        applyResignationScope(wrapper, normalizedScope, userId);
        wrapper.orderByDesc(ResignationApproval::getCreateTime);
        wrapper.last("LIMIT " + safeApprovalListLimit(limit));
        return resignationApprovalMapper.selectList(wrapper).stream().map(this::toResignationVO).toList();
    }

    public ResignationApprovalVO getResignationDetail(String resignationCode) {
        return toResignationVO(getResignationByCode(resignationCode));
    }

    public List<QualityApprovalVO> listQualityApprovals(Integer limit) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long currentUserId = TenantPermissionContext.getUserId();
        int safeLimit = safeApprovalListLimit(limit);
        List<String> approvalCodes = approvalAuditorCandidateService.findPendingApprovalCodes(
                tenantCode, APPROVAL_TYPE_QUALITY, currentUserId);
        if (approvalCodes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> codeSet = new LinkedHashSet<>(approvalCodes);
        return badProductMapper.selectList(new LambdaQueryWrapper<BadProductRecord>()
                        .in(BadProductRecord::getDefectiveId, approvalCodes)
                        .orderByDesc(BadProductRecord::getUpdateTime)
                        .last("LIMIT " + safeLimit))
                .stream()
                .filter(record -> record != null && codeSet.contains(record.getDefectiveId()))
                .filter(record -> BAD_PRODUCT_STATUS_PENDING_AUDIT.equals(record.getStatus()))
                .map(this::toQualityApprovalVO)
                .filter(row -> Boolean.TRUE.equals(row.getCanAudit()))
                .sorted(Comparator.comparing(QualityApprovalVO::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    public QualityApprovalVO getQualityApprovalDetail(String defectiveId) {
        return toQualityApprovalVO(findQualityForApproval(defectiveId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditQuality(QualityAuditRequest request) {
        BadProductRecord record = findQualityForApproval(request.getDefectiveId());
        String approvalCode = qualityService.qualityApprovalCode(record.getDefectiveId());
        Long currentUserId = TenantPermissionContext.getUserId();
        if (!approvalAuditorCandidateService.isPendingAuditor(
                record.getTenantCode(), APPROVAL_TYPE_QUALITY, approvalCode, currentUserId)) {
            throw new BusinessException("您不是该质量处理的当前审核人");
        }
        String auditComment = trimToNull(request.getComment());
        boolean approve = ApprovalActionEnum.isApprove(request.getAction());
        ApprovalAuditorCandidateService.ApprovalDecision decision = recordCandidateDecision(
                record.getTenantCode(), APPROVAL_TYPE_QUALITY, approvalCode, currentUserId, approve, auditComment);
        if (!approve) {
            qualityService.rejectProcessApproval(record.getDefectiveId());
            approvalAuditorCandidateService.closeActiveCandidates(
                    record.getTenantCode(), APPROVAL_TYPE_QUALITY, approvalCode);
            return;
        }
        if (decision == ApprovalAuditorCandidateService.ApprovalDecision.PENDING) {
            return;
        }
        qualityService.approveProcess(record.getDefectiveId());
        approvalAuditorCandidateService.closeActiveCandidates(
                record.getTenantCode(), APPROVAL_TYPE_QUALITY, approvalCode);
    }

    public List<OrderApprovalVO> listOrderApprovals(Integer limit) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long currentUserId = TenantPermissionContext.getUserId();
        int safeLimit = safeApprovalListLimit(limit);
        List<OrderApprovalVO> salesRows = salesOrderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                        .in(SalesOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM, ORDER_STATUS_PENDING_PAY,
                                ORDER_STATUS_PENDING_SHIP, ORDER_STATUS_PENDING_CANCEL)
                        .orderByDesc(SalesOrder::getCreateTime)
                        .last("LIMIT " + safeLimit))
                .stream()
                .filter(this::hasActiveSalesOrderApproval)
                .map(this::toSalesOrderApprovalVO)
                .toList();
        List<OrderApprovalVO> productionRows = productionOrderMapper.selectList(new LambdaQueryWrapper<ProductionOrder>()
                        .in(ProductionOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM, ORDER_STATUS_PENDING_PAY)
                        .orderByDesc(ProductionOrder::getCreateTime)
                        .last("LIMIT " + safeLimit))
                .stream()
                .filter(this::hasActiveProductionOrderApproval)
                .map(this::toProductionOrderApprovalVO)
                .toList();
        List<OrderApprovalVO> rollbackRows = approvalAuditorCandidateService
                .findPendingApprovalCodes(tenantCode, APPROVAL_TYPE_ORDER, currentUserId)
                .stream()
                .filter(code -> code != null && code.startsWith(ORDER_TYPE_SALES + ":"))
                .map(code -> code.substring((ORDER_TYPE_SALES + ":").length()))
                .distinct()
                .map(this::findSalesOrderForApproval)
                .filter(order -> orderService.hasPendingSalesRollbackApproval(order.getOrderId()))
                .map(this::toSalesOrderApprovalVO)
                .toList();
        List<OrderApprovalVO> productionRollbackRows = approvalAuditorCandidateService
                .findPendingApprovalCodes(tenantCode, APPROVAL_TYPE_ORDER, currentUserId)
                .stream()
                .filter(code -> code != null && code.startsWith(ORDER_TYPE_PRODUCTION + ":"))
                .map(code -> code.substring((ORDER_TYPE_PRODUCTION + ":").length()))
                .distinct()
                .map(this::findProductionOrderForApprovalOrNull)
                .filter(order -> order != null && orderService.hasPendingProductionRollbackApproval(order.getOrderId()))
                .map(this::toProductionOrderApprovalVO)
                .toList();

        LinkedHashSet<String> seenOrderKeys = new LinkedHashSet<>();
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(java.util.stream.Stream.concat(salesRows.stream(), productionRows.stream()), rollbackRows.stream()),
                        productionRollbackRows.stream())
                .filter(row -> Boolean.TRUE.equals(row.getCanAudit()))
                .filter(row -> seenOrderKeys.add(row.getOrderType() + ":" + row.getOrderId()))
                .sorted(Comparator.comparing(OrderApprovalVO::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    public OrderApprovalVO getOrderApprovalDetail(String orderType, String orderId) {
        if (ORDER_TYPE_SALES.equalsIgnoreCase(orderType)) {
            return toSalesOrderApprovalVO(findSalesOrderForApproval(orderId));
        }
        if (ORDER_TYPE_PRODUCTION.equalsIgnoreCase(orderType)) {
            return toProductionOrderApprovalVO(findProductionOrderForApproval(orderId));
        }
        throw new BusinessException("订单审批类型不合法");
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditOrder(OrderApprovalAuditRequest request) {
        String orderType = request.getOrderType() == null ? "" : request.getOrderType().trim().toLowerCase();
        String remark = StringUtils.hasText(request.getComment()) ? request.getComment().trim() : "审批中心确认订单";

        if (ORDER_TYPE_SALES.equals(orderType)) {
            SalesOrder salesOrder = findSalesOrderForApproval(request.getOrderId());
            String requiredPermission = resolveOrderAuditPermissionCode(salesOrder);
            assertOrderAuditPermission(requiredPermission);
            validateCurrentOrderAuditor(orderType, salesOrder.getOrderId(), salesOrder.getTenantCode(), requiredPermission);
            String approvalCode = orderApprovalCode(orderType, salesOrder.getOrderId());
            boolean approve = ApprovalActionEnum.isApprove(request.getAction());
            ApprovalAuditorCandidateService.ApprovalDecision decision = recordCandidateDecision(
                    salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode, TenantPermissionContext.getUserId(), approve, remark);
            if (!ApprovalActionEnum.isApprove(request.getAction())) {
                if (orderService.hasPendingSalesRollbackApproval(salesOrder.getOrderId())) {
                    approvalAuditorCandidateService.closeActiveCandidates(salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                    return;
                }
                if (ORDER_STATUS_PENDING_CANCEL.equals(salesOrder.getStatus())) {
                    orderService.rejectPendingCancelSalesOrder(request.getOrderId(), remark);
                    approvalAuditorCandidateService.closeActiveCandidates(salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                    return;
                }
                if (ORDER_STATUS_PENDING_PAY.equals(salesOrder.getStatus())
                        || ORDER_STATUS_PENDING_SHIP.equals(salesOrder.getStatus())) {
                    approvalAuditorCandidateService.closeActiveCandidates(salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                    return;
                }
                throw new BusinessException("订单驳回/取消请到订单管理中处理，避免误改业务单据状态");
            }
            if (decision == ApprovalAuditorCandidateService.ApprovalDecision.PENDING) {
                return;
            }
            if (orderService.hasPendingSalesRollbackApproval(salesOrder.getOrderId())) {
                if (isCompletedDrawingBudgetOrder(salesOrder)) {
                    approvalAuditorCandidateService.closeActiveCandidates(salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                    return;
                }
                orderService.approveSalesOrderRollback(request.getOrderId(), remark);
                approvalAuditorCandidateService.closeActiveCandidates(salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                return;
            }
            if (ORDER_STATUS_PENDING_CANCEL.equals(salesOrder.getStatus()) && isDrawingBudgetOrder(salesOrder)) {
                orderService.rejectPendingCancelSalesOrder(request.getOrderId(), remark);
                approvalAuditorCandidateService.closeActiveCandidates(salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                return;
            }
            SalesOrderUpdateRequest updateRequest = new SalesOrderUpdateRequest();
            updateRequest.setStatus(resolveSalesApprovalNextStatus(salesOrder));
            updateRequest.setRemark(remark);
            orderService.approveSalesOrderTransition(request.getOrderId(), updateRequest.getStatus(), updateRequest.getRemark());
            approvalAuditorCandidateService.closeActiveCandidates(salesOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
            return;
        }
        if (ORDER_TYPE_PRODUCTION.equals(orderType)) {
            ProductionOrder productionOrder = findProductionOrderForApproval(request.getOrderId());
            String requiredPermission = resolveOrderAuditPermissionCode(productionOrder);
            assertOrderAuditPermission(requiredPermission);
            validateCurrentOrderAuditor(orderType, productionOrder.getOrderId(), productionOrder.getTenantCode(), requiredPermission);
            String approvalCode = orderApprovalCode(orderType, productionOrder.getOrderId());
            boolean approve = ApprovalActionEnum.isApprove(request.getAction());
            ApprovalAuditorCandidateService.ApprovalDecision decision = recordCandidateDecision(
                    productionOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode, TenantPermissionContext.getUserId(), approve, remark);
            if (!approve) {
                if (orderService.hasPendingProductionRollbackApproval(productionOrder.getOrderId())) {
                    approvalAuditorCandidateService.closeActiveCandidates(productionOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                    return;
                }
                throw new BusinessException("订单驳回/取消请到订单管理中处理，避免误改业务单据状态");
            }
            if (decision == ApprovalAuditorCandidateService.ApprovalDecision.PENDING) {
                return;
            }
            if (orderService.hasPendingProductionRollbackApproval(productionOrder.getOrderId())) {
                orderService.approveProductionOrderRollback(request.getOrderId(), remark);
                approvalAuditorCandidateService.closeActiveCandidates(productionOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
                return;
            }
            String targetStatus = resolveProductionApprovalNextStatus(productionOrder);
            orderService.approveProductionOrderTransition(request.getOrderId(), targetStatus, remark);
            approvalAuditorCandidateService.closeActiveCandidates(productionOrder.getTenantCode(), APPROVAL_TYPE_ORDER, approvalCode);
            return;
        }
        throw new BusinessException("订单审批类型不合法");
    }

    @Transactional(rollbackFor = Exception.class)
    public String submitResignation(ResignationSubmitRequest request) {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();

        Long pendingCount = resignationApprovalMapper.selectCount(new LambdaQueryWrapper<ResignationApproval>()
                .eq(ResignationApproval::getApplyUserId, userId)
                .eq(ResignationApproval::getStatus, ApprovalStatusEnum.PENDING.getCode()));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("已有待审批离职申请，请勿重复提交");
        }

        ResignationApproval approval = new ResignationApproval();
        approval.setResignationCode(codeGeneratorUtil.generateResignationApprovalCode());
        approval.setTenantCode(tenantCode);
        approval.setApplyUserId(userId);
        approval.setExpectedLeaveDate(request.getExpectedLeaveDate());
        approval.setReason(request.getReason().trim());
        approval.setHandoverNote(trimToNull(request.getHandoverNote()));
        approval.setStatus(ApprovalStatusEnum.PENDING.getCode());
        assignResignationAuditors(approval, request.getAuditorId(), request.getAuditorIds(), true);
        resignationApprovalMapper.insert(approval);
        return approval.getResignationCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditResignation(ResignationAuditRequest request) {
        Long currentUserId = TenantPermissionContext.getUserId();
        ResignationApproval approval = getResignationByCode(request.getResignationCode());
        if (!canCurrentUserAudit(currentUserId, approval.getAuditorId(), approval.getAuditorIds())) {
            throw new BusinessException("您不是该离职审批单当前审批人");
        }
        if (!ApprovalStatusEnum.isPending(approval.getStatus())) {
            throw new BusinessException("该离职审批单已处理，请勿重复审批");
        }

        String auditComment = trimToNull(request.getComment());
        boolean approve = ApprovalActionEnum.isApprove(request.getAction());
        ApprovalAuditorCandidateService.ApprovalDecision decision = recordCandidateDecision(
                approval.getTenantCode(), APPROVAL_TYPE_RESIGNATION, approval.getResignationCode(), currentUserId, approve, auditComment);
        if (decision.isCandidateFlow()) {
            approval.setAuditComment(auditComment);
            if (!approve) {
                approval.setStatus(ApprovalStatusEnum.REJECTED.getCode());
                approvalAuditorCandidateService.closeActiveCandidates(
                        approval.getTenantCode(), APPROVAL_TYPE_RESIGNATION, approval.getResignationCode());
                resignationApprovalMapper.updateById(approval);
                return;
            }
            if (decision == ApprovalAuditorCandidateService.ApprovalDecision.PENDING) {
                resignationApprovalMapper.updateById(approval);
                return;
            }
            approval.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            employeeService.markResignedByApproval(approval.getApplyUserId(), approval.getReason());
            approvalAuditorCandidateService.closeActiveCandidates(
                    approval.getTenantCode(), APPROVAL_TYPE_RESIGNATION, approval.getResignationCode());
            resignationApprovalMapper.updateById(approval);
            return;
        }
        approval.setAuditComment(auditComment);
        if (approve) {
            Long nextManagerId = getManagerId(currentUserId);
            Integer roleLevel = getRoleLevel(currentUserId);
            if ((roleLevel != null && roleLevel >= 2) || nextManagerId == null) {
                approval.setStatus(ApprovalStatusEnum.APPROVED.getCode());
                employeeService.markResignedByApproval(approval.getApplyUserId(), approval.getReason());
            } else {
                assignResignationAuditors(approval, nextManagerId);
            }
        } else {
            approval.setStatus(ApprovalStatusEnum.REJECTED.getCode());
        }
        if (!ApprovalStatusEnum.isPending(approval.getStatus())) {
            approvalAuditorCandidateService.closeActiveCandidates(
                    approval.getTenantCode(), APPROVAL_TYPE_RESIGNATION, approval.getResignationCode());
        }
        resignationApprovalMapper.updateById(approval);
    }

    @Transactional(rollbackFor = Exception.class)
    public String submitFinance(FinanceSubmitRequest request) {
        Long userId = TenantPermissionContext.getUserId();

        FinanceApproval approval = new FinanceApproval();
        approval.setApprovalCode(codeGeneratorUtil.generateCode("FIN", 4));
        String tenantCode = TenantPermissionContext.getTenantCode();
        approval.setTenantCode(tenantCode);
        approval.setApplyUserId(userId);
        approval.setCategory(request.getCategory().trim());
        approval.setAmount(request.getAmount());
        approval.setReason(request.getReason().trim());
        String normalizedAttachmentUrl = InternalUploadUrlValidator.normalizeOptionalFinanceAttachment(
                request.getAttachmentUrl(),
                tenantCode,
                resolveContextPath()
        );
        approval.setAttachmentName(normalizeAttachmentName(request.getAttachmentName(), normalizedAttachmentUrl));
        approval.setAttachmentUrl(normalizedAttachmentUrl);
        approval.setAttachmentSize(normalizeAttachmentSize(request.getAttachmentSize(), normalizedAttachmentUrl));
        approval.setStatus(ApprovalStatusEnum.PENDING.getCode());
        assignFinanceAuditors(approval, request.getAuditorId(), request.getAuditorIds(), true);
        financeApprovalMapper.insert(approval);
        return approval.getApprovalCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditFinance(FinanceAuditRequest request) {
        Long currentUserId = TenantPermissionContext.getUserId();
        FinanceApproval approval = getFinanceByCode(request.getApprovalCode());
        if (!canCurrentUserAudit(currentUserId, approval.getAuditorId(), approval.getAuditorIds())) {
            throw new BusinessException("您不是该财务审批单当前审批人");
        }
        if (!ApprovalStatusEnum.isPending(approval.getStatus())) {
            throw new BusinessException("该财务审批单已处理，请勿重复审批");
        }

        String auditComment = trimToNull(request.getComment());
        boolean approve = ApprovalActionEnum.isApprove(request.getAction());
        ApprovalAuditorCandidateService.ApprovalDecision decision = recordCandidateDecision(
                approval.getTenantCode(), APPROVAL_TYPE_FINANCE, approval.getApprovalCode(), currentUserId, approve, auditComment);
        if (decision.isCandidateFlow()) {
            approval.setAuditComment(auditComment);
            if (!approve) {
                approval.setStatus(ApprovalStatusEnum.REJECTED.getCode());
                approvalAuditorCandidateService.closeActiveCandidates(
                        approval.getTenantCode(), APPROVAL_TYPE_FINANCE, approval.getApprovalCode());
                financeApprovalMapper.updateById(approval);
                return;
            }
            if (decision == ApprovalAuditorCandidateService.ApprovalDecision.PENDING) {
                financeApprovalMapper.updateById(approval);
                return;
            }
            approval.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            approvalAuditorCandidateService.closeActiveCandidates(
                    approval.getTenantCode(), APPROVAL_TYPE_FINANCE, approval.getApprovalCode());
            financeApprovalMapper.updateById(approval);
            return;
        }
        approval.setAuditComment(auditComment);
        if (approve) {
            Long nextManagerId = getManagerId(currentUserId);
            Integer roleLevel = getRoleLevel(currentUserId);
            if ((roleLevel != null && roleLevel >= 2) || nextManagerId == null) {
                approval.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            } else {
                assignFinanceAuditors(approval, nextManagerId);
            }
        } else {
            approval.setStatus(ApprovalStatusEnum.REJECTED.getCode());
        }
        if (!ApprovalStatusEnum.isPending(approval.getStatus())) {
            approvalAuditorCandidateService.closeActiveCandidates(
                    approval.getTenantCode(), APPROVAL_TYPE_FINANCE, approval.getApprovalCode());
        }
        financeApprovalMapper.updateById(approval);
    }

    private UserLeave getLeaveByCode(String leaveCode) {
        UserLeave userLeave = leaveMapper.selectOne(new LambdaQueryWrapper<UserLeave>()
                .eq(UserLeave::getLeaveCode, leaveCode));
        if (userLeave == null) {
            throw new BusinessException("请假单不存在");
        }
        return userLeave;
    }

    private FinanceApproval getFinanceByCode(String approvalCode) {
        FinanceApproval approval = financeApprovalMapper.selectOne(new LambdaQueryWrapper<FinanceApproval>()
                .eq(FinanceApproval::getApprovalCode, approvalCode));
        if (approval == null) {
            throw new BusinessException("财务审批单不存在");
        }
        return approval;
    }

    private ResignationApproval getResignationByCode(String resignationCode) {
        ResignationApproval approval = resignationApprovalMapper.selectOne(new LambdaQueryWrapper<ResignationApproval>()
                .eq(ResignationApproval::getResignationCode, resignationCode));
        if (approval == null) {
            throw new BusinessException("离职审批单不存在");
        }
        return approval;
    }

    private LeaveApprovalListVO toLeaveListVO(UserLeave leave) {
        LeaveApprovalListVO vo = new LeaveApprovalListVO();
        BeanUtils.copyProperties(leave, vo);
        vo.setLeaveTypeText(leaveTypeText(leave.getLeaveType()));
        vo.setStatusText(statusText(leave.getStatus()));
        Employee applyUser = employeeMapper.selectById(leave.getApplyUserId());
        if (applyUser != null) {
            vo.setApplyUserName(applyUser.getName());
            vo.setApplyDepartmentName(applyUser.getDepartmentName());
        }
        vo.setAuditorName(resolveAuditorNames(leave.getAuditorId(), leave.getAuditorIds()));
        return vo;
    }

    private FinanceApprovalVO toFinanceVO(FinanceApproval approval) {
        FinanceApprovalVO vo = new FinanceApprovalVO();
        BeanUtils.copyProperties(approval, vo);
        vo.setStatusText(statusText(approval.getStatus()));
        Employee applyUser = employeeMapper.selectById(approval.getApplyUserId());
        if (applyUser != null) {
            vo.setApplyUserName(applyUser.getName());
            vo.setApplyDepartmentName(applyUser.getDepartmentName());
        }
        vo.setAuditorName(resolveAuditorNames(approval.getAuditorId(), approval.getAuditorIds()));
        return vo;
    }

    private ResignationApprovalVO toResignationVO(ResignationApproval approval) {
        ResignationApprovalVO vo = new ResignationApprovalVO();
        BeanUtils.copyProperties(approval, vo);
        vo.setStatusText(statusText(approval.getStatus()));
        Employee applyUser = employeeMapper.selectById(approval.getApplyUserId());
        if (applyUser != null) {
            vo.setApplyUserName(applyUser.getName());
            vo.setApplyDepartmentName(applyUser.getDepartmentName());
        }
        vo.setAuditorName(resolveAuditorNames(approval.getAuditorId(), approval.getAuditorIds()));
        return vo;
    }

    private QualityApprovalVO toQualityApprovalVO(BadProductRecord record) {
        QualityApprovalVO vo = new QualityApprovalVO();
        vo.setDefectiveId(record.getDefectiveId());
        vo.setOrderId(record.getOrderId());
        vo.setType(record.getType());
        vo.setTypeText(qualityTypeText(record.getType()));
        vo.setApplicantName(StringUtils.hasText(record.getCreatorName()) ? record.getCreatorName() : "质量处理");
        vo.setQuantity(record.getQuantity());
        vo.setLossAmount(record.getLossAmount());
        vo.setDescription(record.getDescription());
        vo.setResponsiblePerson(record.getResponsiblePerson());
        vo.setProcessMeasure(record.getProcessMeasure());
        vo.setImprovementPlan(record.getImprovementPlan());
        vo.setProcessMethod(record.getProcessMethod());
        vo.setProcessRemark(record.getProcessRemark());
        vo.setStatus(BAD_PRODUCT_STATUS_PROCESSED.equals(record.getStatus())
                ? ApprovalStatusEnum.APPROVED.getCode()
                : ApprovalStatusEnum.PENDING.getCode());
        vo.setStatusText(qualityStatusText(record.getStatus()));
        vo.setSummary(buildQualitySummary(record));
        vo.setCreateTime(record.getUpdateTime() == null ? record.getCreateTime() : record.getUpdateTime());
        applyQualityAuditor(vo, record);
        return vo;
    }

    private BadProductRecord findQualityForApproval(String defectiveId) {
        if (!StringUtils.hasText(defectiveId)) {
            throw new BusinessException("质量编号不能为空");
        }
        BadProductRecord record = badProductMapper.selectOne(new LambdaQueryWrapper<BadProductRecord>()
                .eq(BadProductRecord::getDefectiveId, defectiveId.trim())
                .last("LIMIT 1"));
        if (record == null || !BAD_PRODUCT_STATUS_PENDING_AUDIT.equals(record.getStatus())) {
            throw new BusinessException("待审核质量处理不存在或已处理");
        }
        return record;
    }

    private void applyQualityAuditor(QualityApprovalVO vo, BadProductRecord record) {
        String approvalCode = qualityService.qualityApprovalCode(record.getDefectiveId());
        List<Long> auditorIds = approvalAuditorCandidateService.findPendingAuditorIds(
                record.getTenantCode(), APPROVAL_TYPE_QUALITY, approvalCode);
        if (auditorIds.isEmpty()) {
            auditorIds = approvalAuditorCandidateService.findActiveAuditorIds(
                    record.getTenantCode(), APPROVAL_TYPE_QUALITY, approvalCode);
        }
        vo.setAuditorId(auditorIds.isEmpty() ? null : auditorIds.get(0));
        vo.setAuditorIds(joinAuditorIds(auditorIds));
        vo.setAuditorName(resolveOrderAuditorNames(auditorIds));
        vo.setCanAudit(approvalAuditorCandidateService.isPendingAuditor(
                record.getTenantCode(), APPROVAL_TYPE_QUALITY, approvalCode, TenantPermissionContext.getUserId()));
    }

    private String buildQualitySummary(BadProductRecord record) {
        List<String> parts = new ArrayList<>();
        parts.add(qualityTypeText(record.getType()));
        if (StringUtils.hasText(record.getOrderId())) {
            parts.add("订单 " + record.getOrderId());
        }
        if (record.getQuantity() != null) {
            parts.add("数量 " + record.getQuantity());
        }
        if (StringUtils.hasText(record.getProcessMethod())) {
            parts.add(record.getProcessMethod());
        }
        return String.join(" / ", parts);
    }

    private OrderApprovalVO toSalesOrderApprovalVO(SalesOrder order) {
        boolean cancelApproval = ORDER_STATUS_PENDING_CANCEL.equals(order.getStatus());
        boolean rollbackApproval = orderService.hasPendingSalesRollbackApproval(order.getOrderId());
        SalesOrderStatusLog rollbackLog = rollbackApproval ? orderService.findPendingSalesRollbackLog(order.getOrderId()) : null;
        boolean payToProductionApproval = ORDER_STATUS_PENDING_PAY.equals(order.getStatus());
        boolean shipmentApproval = ORDER_STATUS_PENDING_SHIP.equals(order.getStatus());
        boolean specialCreateApproval = ORDER_STATUS_PENDING_CONFIRM.equals(order.getStatus())
                && OrderCategoryEnum.SPECIAL_ORDER.getCode().equals(OrderCategoryEnum.normalize(order.getOrderCategory()));
        OrderApprovalVO vo = new OrderApprovalVO();
        vo.setOrderType(ORDER_TYPE_SALES);
        vo.setOrderTypeText("销售订单");
        vo.setOrderId(order.getOrderId());
        vo.setCustomerName(order.getCustomerName());
        vo.setProjectName(order.getProjectName());
        String fallbackSummary = specialCreateApproval
                ? "特殊订单创建审核"
                : (payToProductionApproval ? "待审批转备料中销售订单" : "待确认销售订单");
        if (cancelApproval) {
            fallbackSummary = "取消订单审核";
        }
        if (shipmentApproval) {
            fallbackSummary = "待审批转已发货销售订单";
        }
        if (rollbackApproval && rollbackLog != null) {
            fallbackSummary = "订单回退审核：" + statusLabel(rollbackLog.getOldStatus()) + " → " + statusLabel(rollbackLog.getNewStatus());
        }
        vo.setSummary(StringUtils.hasText(order.getGoodsDesc()) ? order.getGoodsDesc() : fallbackSummary);
        vo.setStatus(order.getStatus());
        vo.setStatusText(specialCreateApproval ? "待审核创建"
                : (shipmentApproval ? "待审批转已发货" : (payToProductionApproval ? "待审批转备料中" : "待确认")));
        if (cancelApproval) {
            vo.setStatusText("待审核取消");
        }
        if (rollbackApproval && rollbackLog != null) {
            vo.setStatusText("待审核回退至" + statusLabel(rollbackLog.getNewStatus()));
        }
        vo.setCreateTime(order.getCreateTime());
        applyOrderAuditor(vo, order.getTenantCode(), ORDER_TYPE_SALES, order.getOrderId(),
                resolveOrderAuditPermissionCode(order));
        return vo;
    }

    private OrderApprovalVO toProductionOrderApprovalVO(ProductionOrder order) {
        boolean rollbackApproval = orderService.hasPendingProductionRollbackApproval(order.getOrderId());
        ProductionOrderStatusLog rollbackLog = rollbackApproval ? orderService.findPendingProductionRollbackLog(order.getOrderId()) : null;
        boolean payToMaterialApproval = ORDER_STATUS_PENDING_PAY.equals(order.getStatus());
        OrderApprovalVO vo = new OrderApprovalVO();
        vo.setOrderType(ORDER_TYPE_PRODUCTION);
        vo.setOrderTypeText("订单");
        vo.setOrderId(order.getOrderId());
        vo.setCustomerName(order.getCustomerName());
        vo.setProjectName(order.getProjectName());
        String fallbackSummary = (StringUtils.hasText(order.getModelCode()) ? order.getModelCode() : (payToMaterialApproval ? "待审批转备料中订单" : "待确认订单"))
                + " / 数量 " + (order.getQuantity() == null ? 0 : order.getQuantity());
        if (rollbackApproval && rollbackLog != null) {
            fallbackSummary = "订单回退审核：" + statusLabel(rollbackLog.getOldStatus()) + " → " + statusLabel(rollbackLog.getNewStatus());
        }
        vo.setSummary(fallbackSummary);
        vo.setStatus(order.getStatus());
        vo.setStatusText(rollbackApproval && rollbackLog != null ? "待审核回退至" + statusLabel(rollbackLog.getNewStatus()) : (payToMaterialApproval ? "待审批转备料中" : "待确认"));
        vo.setCreateTime(order.getCreateTime());
        applyOrderAuditor(vo, order.getTenantCode(), ORDER_TYPE_PRODUCTION, order.getOrderId(),
                resolveOrderAuditPermissionCode(order));
        return vo;
    }

    private SalesOrder findPendingSalesOrder(String orderId) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderId, orderId)
                .in(SalesOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM, ORDER_STATUS_PENDING_PAY,
                        ORDER_STATUS_PENDING_SHIP, ORDER_STATUS_PENDING_CANCEL));
        if (order == null) {
            throw new BusinessException("待审批销售订单不存在或已处理");
        }
        return order;
    }

    private SalesOrder findSalesOrderForApproval(String orderId) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderId, orderId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("待审批销售订单不存在或已处理");
        }
        boolean eligibleStatus = ORDER_STATUS_PENDING_CONFIRM.equals(order.getStatus())
                || ORDER_STATUS_PENDING_PAY.equals(order.getStatus())
                || ORDER_STATUS_PENDING_CANCEL.equals(order.getStatus())
                || ORDER_STATUS_PENDING_SHIP.equals(order.getStatus());
        if ((eligibleStatus && hasActiveSalesOrderApproval(order))
                || orderService.hasPendingSalesRollbackApproval(order.getOrderId())) {
            return order;
        }
        throw new BusinessException("待审批销售订单不存在或已处理");
    }

    private boolean hasActiveSalesOrderApproval(SalesOrder order) {
        return order != null && hasActiveOrderApproval(
                order.getTenantCode(), ORDER_TYPE_SALES, order.getOrderId());
    }

    private boolean hasActiveProductionOrderApproval(ProductionOrder order) {
        return order != null && hasActiveOrderApproval(
                order.getTenantCode(), ORDER_TYPE_PRODUCTION, order.getOrderId());
    }

    private boolean hasActiveOrderApproval(String tenantCode, String orderType, String orderId) {
        List<Long> auditorIds = approvalAuditorCandidateService.findActiveAuditorIds(
                tenantCode, APPROVAL_TYPE_ORDER, orderApprovalCode(orderType, orderId));
        return auditorIds != null && !auditorIds.isEmpty();
    }

    private boolean isDrawingBudgetOrder(SalesOrder order) {
        return order != null && OrderCategoryEnum.DRAWING_BUDGET.getCode().equals(
                OrderCategoryEnum.normalize(order.getOrderCategory()));
    }

    private boolean isCompletedDrawingBudgetOrder(SalesOrder order) {
        return isDrawingBudgetOrder(order) && "budget_completed".equals(order.getStatus());
    }

    private ProductionOrder findPendingProductionOrder(String orderId) {
        ProductionOrder order = productionOrderMapper.selectOne(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getOrderId, orderId)
                .in(ProductionOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM, ORDER_STATUS_PENDING_PAY));
        if (order == null) {
            throw new BusinessException("待确认生产订单不存在或已处理");
        }
        return order;
    }

    private ProductionOrder findProductionOrderForApprovalOrNull(String orderId) {
        try {
            return findProductionOrderForApproval(orderId);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private ProductionOrder findProductionOrderForApproval(String orderId) {
        ProductionOrder order = productionOrderMapper.selectOne(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getOrderId, orderId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("待审批生产订单不存在或已处理");
        }
        boolean eligibleStatus = ORDER_STATUS_PENDING_CONFIRM.equals(order.getStatus())
                || ORDER_STATUS_PENDING_PAY.equals(order.getStatus());
        if ((eligibleStatus && hasActiveProductionOrderApproval(order))
                || orderService.hasPendingProductionRollbackApproval(order.getOrderId())) {
            return order;
        }
        throw new BusinessException("待审批生产订单不存在或已处理");
    }

    private long countPendingOrderApprovals(String tenantCode, Long currentUserId) {
        if (!StringUtils.hasText(tenantCode) || currentUserId == null) {
            return 0L;
        }
        return approvalAuditorCandidateService.countPendingAudits(tenantCode, APPROVAL_TYPE_ORDER, currentUserId);
    }

    private void applyOrderAuditor(OrderApprovalVO vo,
                                   String tenantCode,
                                   String orderType,
                                   String orderId,
                                   String requiredPermission) {
        List<Long> auditorIds = approvalAuditorCandidateService.findActiveAuditorIds(
                tenantCode, APPROVAL_TYPE_ORDER, orderApprovalCode(orderType, orderId));
        if (auditorIds == null) {
            auditorIds = List.of();
        }
        Long auditorId = auditorIds.isEmpty() ? null : auditorIds.get(0);
        vo.setAuditorId(auditorId);
        vo.setAuditorIds(joinAuditorIds(auditorIds));
        vo.setAuditorName(resolveOrderAuditorNames(auditorIds));
        vo.setCanAudit(TenantPermissionContext.hasPermission(requiredPermission)
                && approvalAuditorCandidateService.isPendingAuditor(
                tenantCode, APPROVAL_TYPE_ORDER, orderApprovalCode(orderType, orderId), TenantPermissionContext.getUserId()));
    }

    private void validateCurrentOrderAuditor(String orderType,
                                             String orderId,
                                             String tenantCode,
                                             String requiredPermission) {
        Long currentUserId = TenantPermissionContext.getUserId();
        if (!approvalAuditorCandidateService.isPendingAuditor(
                tenantCode, APPROVAL_TYPE_ORDER, orderApprovalCode(orderType, orderId), currentUserId)) {
            throw new BusinessException("您不是该订单的当前审批人");
        }
    }

    private String orderApprovalCode(String orderType, String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException("订单编号不能为空");
        }
        String type = ORDER_TYPE_PRODUCTION.equalsIgnoreCase(orderType) ? ORDER_TYPE_PRODUCTION : ORDER_TYPE_SALES;
        return type + ":" + orderId.trim();
    }

    private String resolveOrderAuditPermissionCode(SalesOrder order) {
        if (order != null && ORDER_STATUS_PENDING_CANCEL.equals(order.getStatus())) {
            return PermissionCatalogV3.CODE_ORDER_AUDIT_CANCEL;
        }
        if (order != null && "pending_pay".equals(order.getStatus())) {
            return PermissionCatalogV3.CODE_ORDER_AUDIT_MATERIAL;
        }
        return PermissionCatalogV3.CODE_ORDER_AUDIT_SHIPMENT;
    }

    private String resolveOrderAuditPermissionCode(ProductionOrder order) {
        return PermissionCatalogV3.CODE_ORDER_AUDIT_SHIPMENT;
    }

    private void assertOrderAuditPermission(String permissionCode) {
        if (!TenantPermissionContext.hasPermission(permissionCode)) {
            throw new BusinessException(403, "您没有权限处理该类型的订单审批");
        }
    }

    private String resolveOrderAuditorName(Long auditorId) {
        if (auditorId == null || auditorId <= 0) {
            return "待分配";
        }
        Employee auditor = employeeMapper.selectById(auditorId);
        return auditor == null || !StringUtils.hasText(auditor.getName()) ? "待分配" : auditor.getName();
    }

    private String resolveOrderAuditorNames(List<Long> auditorIds) {
        if (auditorIds == null || auditorIds.isEmpty()) {
            return "待分配";
        }
        List<String> names = new ArrayList<>();
        for (Long auditorId : auditorIds) {
            Employee auditor = auditorId == null ? null : employeeMapper.selectById(auditorId);
            if (auditor != null && StringUtils.hasText(auditor.getName())) {
                names.add(auditor.getName());
            }
        }
        return names.isEmpty() ? "待分配" : String.join("、", names);
    }

    private void appendLeaveAuditorFilter(LambdaQueryWrapper<UserLeave> wrapper, Long userId) {
        if (userId == null) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(q -> q.eq(UserLeave::getAuditorId, userId)
                .or()
                .apply("FIND_IN_SET({0}, auditor_ids) > 0", String.valueOf(userId)));
    }

    private void appendFinanceAuditorFilter(LambdaQueryWrapper<FinanceApproval> wrapper, Long userId) {
        if (userId == null) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(q -> q.eq(FinanceApproval::getAuditorId, userId)
                .or()
                .apply("FIND_IN_SET({0}, auditor_ids) > 0", String.valueOf(userId)));
    }

    private void appendResignationAuditorFilter(LambdaQueryWrapper<ResignationApproval> wrapper, Long userId) {
        if (userId == null) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(q -> q.eq(ResignationApproval::getAuditorId, userId)
                .or()
                .apply("FIND_IN_SET({0}, auditor_ids) > 0", String.valueOf(userId)));
    }

    private String normalizeApprovalScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "pending";
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mine", "pending", "self_pending", "others_pending", "all" -> normalized;
            default -> "pending";
        };
    }

    private String requireApprovalListScope(String scope,
                                            String submitPermission,
                                            String listPermission,
                                            String auditPermission,
                                            String label) {
        String normalizedScope = normalizeApprovalScope(scope);
        boolean canCreate = TenantPermissionContext.hasPermission(submitPermission);
        boolean canList = TenantPermissionContext.hasPermission(listPermission);
        boolean canAudit = TenantPermissionContext.hasPermission(auditPermission);
        if ("mine".equals(normalizedScope)) {
            if (canCreate || canList || canAudit) {
                return normalizedScope;
            }
            throw new BusinessException(403, "您没有权限查看本人" + label + "审批");
        }
        if ("all".equals(normalizedScope)) {
            if (canList) {
                return normalizedScope;
            }
            throw new BusinessException(403, "您没有权限查看全部" + label + "审批");
        }
        if (canList || canAudit) {
            return normalizedScope;
        }
        throw new BusinessException(403, "您没有权限查看待处理" + label + "审批");
    }

    private void applyLeaveScope(LambdaQueryWrapper<UserLeave> wrapper, String scope, Long userId) {
        if ("mine".equals(scope)) {
            wrapper.eq(UserLeave::getApplyUserId, userId);
        } else if ("self_pending".equals(scope)) {
            wrapper.eq(UserLeave::getApplyUserId, userId)
                    .eq(UserLeave::getStatus, ApprovalStatusEnum.PENDING.getCode());
            appendLeaveAuditorFilter(wrapper, userId);
        } else if ("others_pending".equals(scope)) {
            wrapper.ne(UserLeave::getApplyUserId, userId)
                    .eq(UserLeave::getStatus, ApprovalStatusEnum.PENDING.getCode());
            appendLeaveAuditorFilter(wrapper, userId);
        } else if (!"all".equals(scope)) {
            appendLeaveAuditorFilter(wrapper, userId);
        }
    }

    private void applyFinanceScope(LambdaQueryWrapper<FinanceApproval> wrapper, String scope, Long userId) {
        if ("mine".equals(scope)) {
            wrapper.eq(FinanceApproval::getApplyUserId, userId);
        } else if ("self_pending".equals(scope)) {
            wrapper.eq(FinanceApproval::getApplyUserId, userId)
                    .eq(FinanceApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
            appendFinanceAuditorFilter(wrapper, userId);
        } else if ("others_pending".equals(scope)) {
            wrapper.ne(FinanceApproval::getApplyUserId, userId)
                    .eq(FinanceApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
            appendFinanceAuditorFilter(wrapper, userId);
        } else if (!"all".equals(scope)) {
            appendFinanceAuditorFilter(wrapper, userId);
        }
    }

    private void applyResignationScope(LambdaQueryWrapper<ResignationApproval> wrapper, String scope, Long userId) {
        if ("mine".equals(scope)) {
            wrapper.eq(ResignationApproval::getApplyUserId, userId);
        } else if ("self_pending".equals(scope)) {
            wrapper.eq(ResignationApproval::getApplyUserId, userId)
                    .eq(ResignationApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
            appendResignationAuditorFilter(wrapper, userId);
        } else if ("others_pending".equals(scope)) {
            wrapper.ne(ResignationApproval::getApplyUserId, userId)
                    .eq(ResignationApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
            appendResignationAuditorFilter(wrapper, userId);
        } else if (!"all".equals(scope)) {
            appendResignationAuditorFilter(wrapper, userId);
        }
    }

    private void assignLeaveAuditors(UserLeave approval, Long primaryAuditorId) {
        Long auditorId = resolveSingleAuditorId(
                approval.getTenantCode(),
                approval.getApplyUserId(),
                primaryAuditorId,
                APPROVAL_TYPE_LEAVE,
                PermissionCatalogV3.CODE_APPROVAL_LEAVE_AUDIT,
                false
        );
        applySingleAuditor(approval::setAuditorId, approval::setAuditorIds, auditorId);
        approvalAuditorCandidateService.replaceActiveCandidates(
                approval.getTenantCode(), APPROVAL_TYPE_LEAVE, approval.getLeaveCode(), List.of(auditorId));
    }

    private void assignLeaveAuditors(UserLeave approval,
                                     Long primaryAuditorId,
                                     List<Long> specifiedAuditorIds,
                                     boolean strictPrimary) {
        List<Long> auditorIds = resolveAuditorIds(
                approval.getTenantCode(),
                approval.getApplyUserId(),
                specifiedAuditorIds,
                primaryAuditorId,
                APPROVAL_TYPE_LEAVE,
                PermissionCatalogV3.CODE_APPROVAL_LEAVE_AUDIT,
                strictPrimary
        );
        applyAuditors(approval::setAuditorId, approval::setAuditorIds, auditorIds);
        approvalAuditorCandidateService.replaceActiveCandidates(
                approval.getTenantCode(), APPROVAL_TYPE_LEAVE, approval.getLeaveCode(), auditorIds);
    }

    private void assignFinanceAuditors(FinanceApproval approval, Long primaryAuditorId) {
        assignFinanceAuditors(approval, primaryAuditorId, null, false);
    }

    private void assignFinanceAuditors(FinanceApproval approval, Long primaryAuditorId, boolean strictPrimary) {
        assignFinanceAuditors(approval, primaryAuditorId, null, strictPrimary);
    }

    private void assignFinanceAuditors(FinanceApproval approval,
                                       Long primaryAuditorId,
                                       List<Long> specifiedAuditorIds,
                                       boolean strictPrimary) {
        List<Long> auditorIds = resolveAuditorIds(
                approval.getTenantCode(),
                approval.getApplyUserId(),
                specifiedAuditorIds,
                primaryAuditorId,
                APPROVAL_TYPE_FINANCE,
                PermissionCatalogV3.CODE_APPROVAL_FINANCE_AUDIT,
                strictPrimary
        );
        applyAuditors(approval::setAuditorId, approval::setAuditorIds, auditorIds);
        approvalAuditorCandidateService.replaceActiveCandidates(
                approval.getTenantCode(), APPROVAL_TYPE_FINANCE, approval.getApprovalCode(), auditorIds);
    }

    private void assignResignationAuditors(ResignationApproval approval, Long primaryAuditorId) {
        assignResignationAuditors(approval, primaryAuditorId, null, false);
    }

    private void assignResignationAuditors(ResignationApproval approval, Long primaryAuditorId, boolean strictPrimary) {
        assignResignationAuditors(approval, primaryAuditorId, null, strictPrimary);
    }

    private void assignResignationAuditors(ResignationApproval approval,
                                           Long primaryAuditorId,
                                           List<Long> specifiedAuditorIds,
                                           boolean strictPrimary) {
        List<Long> auditorIds = resolveAuditorIds(
                approval.getTenantCode(),
                approval.getApplyUserId(),
                specifiedAuditorIds,
                primaryAuditorId,
                APPROVAL_TYPE_RESIGNATION,
                PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_AUDIT,
                strictPrimary
        );
        applyAuditors(approval::setAuditorId, approval::setAuditorIds, auditorIds);
        approvalAuditorCandidateService.replaceActiveCandidates(
                approval.getTenantCode(), APPROVAL_TYPE_RESIGNATION, approval.getResignationCode(), auditorIds);
    }

    private Long resolveSingleAuditorId(String tenantCode,
                                        Long applyUserId,
                                        Long primaryAuditorId,
                                        String approvalType,
                                        String permissionCode,
                                        boolean strictPrimary) {
        return approvalDefaultAuditorService.resolveAuditorId(
                tenantCode, approvalType, applyUserId, primaryAuditorId, permissionCode, strictPrimary);
    }

    private List<Long> resolveAuditorIds(String tenantCode,
                                         Long applyUserId,
                                         List<Long> specifiedAuditorIds,
                                         Long primaryAuditorId,
                                         String approvalType,
                                         String permissionCode,
                                         boolean strictPrimary) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (specifiedAuditorIds != null) {
            for (Long id : specifiedAuditorIds) {
                if (id == null || id <= 0) {
                    continue;
                }
                if (applyUserId != null && applyUserId.equals(id)) {
                    throw new BusinessException("审批人不能选择申请人本人");
                }
                ids.add(id);
            }
        }
        if (!ids.isEmpty()) {
            if (ids.size() > MAX_PARALLEL_APPROVERS) {
                throw new BusinessException("审批人不能超过 " + MAX_PARALLEL_APPROVERS + " 人");
            }
            List<Long> permittedIds = employeeMapper.selectActiveApproverIdsByPermission(tenantCode, permissionCode);
            for (Long id : ids) {
                if (permittedIds == null || !permittedIds.contains(id)) {
                    throw new BusinessException("所选审批人没有该审批权限");
                }
            }
            return new ArrayList<>(ids);
        }
        Long auditorId = resolveSingleAuditorId(
                tenantCode, applyUserId, primaryAuditorId, approvalType, permissionCode, strictPrimary);
        return List.of(auditorId);
    }

    private void addCandidateAuditor(LinkedHashSet<Long> ids, Long auditorId, Long applyUserId) {
        if (auditorId == null || auditorId <= 0) {
            return;
        }
        if (applyUserId != null && applyUserId.equals(auditorId)) {
            return;
        }
        ids.add(auditorId);
    }

    private void applySingleAuditor(java.util.function.Consumer<Long> primarySetter,
                                    java.util.function.Consumer<String> idsSetter,
                                    Long auditorId) {
        if (auditorId == null || auditorId <= 0) {
            throw new BusinessException("未找到可用审批人，请先配置审批角色权限");
        }
        primarySetter.accept(auditorId);
        idsSetter.accept(null);
    }

    private void applyAuditors(java.util.function.Consumer<Long> primarySetter,
                               java.util.function.Consumer<String> idsSetter,
                               List<Long> auditorIds) {
        if (auditorIds == null || auditorIds.isEmpty()) {
            throw new BusinessException("未找到可用审批人，请先配置审批角色权限");
        }
        primarySetter.accept(auditorIds.get(0));
        idsSetter.accept(auditorIds.size() > 1 ? joinAuditorIds(auditorIds) : null);
    }

    private ApprovalAuditorCandidateService.ApprovalDecision recordCandidateDecision(String tenantCode,
                                                                                      String approvalType,
                                                                                      String approvalCode,
                                                                                      Long currentUserId,
                                                                                      boolean approved,
                                                                                      String comment) {
        return approvalAuditorCandidateService.recordDecision(
                tenantCode, approvalType, approvalCode, currentUserId, approved, comment);
    }

    private boolean canCurrentUserAudit(Long currentUserId, Long auditorId, String auditorIds) {
        if (currentUserId == null) {
            return false;
        }
        if (currentUserId.equals(auditorId)) {
            return true;
        }
        return parseAuditorIds(auditorIds).contains(currentUserId);
    }

    private String resolveAuditorNames(Long auditorId, String auditorIds) {
        List<Long> ids = parseAuditorIds(auditorIds);
        if (ids.isEmpty() && auditorId != null) {
            ids = List.of(auditorId);
        }
        List<String> names = new ArrayList<>();
        for (Long id : ids) {
            Employee auditor = employeeMapper.selectById(id);
            if (auditor != null && StringUtils.hasText(auditor.getName())) {
                names.add(auditor.getName());
            }
        }
        return names.isEmpty() ? "待分配" : String.join("、", names);
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
                Long id = Long.valueOf(raw.trim());
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // Ignore dirty historical data instead of breaking approval list rendering.
            }
        }
        return new ArrayList<>(ids);
    }

    private String joinAuditorIds(List<Long> auditorIds) {
        if (auditorIds == null || auditorIds.isEmpty()) {
            return null;
        }
        return String.join(",", auditorIds.stream().map(String::valueOf).toList());
    }

    private String resolveSalesApprovalNextStatus(SalesOrder order) {
        if (order == null || !StringUtils.hasText(order.getStatus())) {
            throw new BusinessException("销售订单状态异常，无法审批");
        }
        if (ORDER_STATUS_PENDING_CONFIRM.equals(order.getStatus())) {
            return SALES_APPROVED_NEXT_STATUS;
        }
        if (ORDER_STATUS_PENDING_PAY.equals(order.getStatus())) {
            return ORDER_STATUS_PENDING_MATERIAL;
        }
        if (ORDER_STATUS_PENDING_SHIP.equals(order.getStatus())) {
            return ORDER_STATUS_SHIPPED;
        }
        if (ORDER_STATUS_PENDING_CANCEL.equals(order.getStatus())) {
            return ORDER_STATUS_CANCELLED;
        }
        throw new BusinessException("当前销售订单状态无需审批");
    }

    private String resolveProductionApprovalNextStatus(ProductionOrder order) {
        if (order == null || !StringUtils.hasText(order.getStatus())) {
            throw new BusinessException("订单状态异常，无法审批");
        }
        if (ORDER_STATUS_PENDING_CONFIRM.equals(order.getStatus())) {
            return ORDER_STATUS_PENDING_PAY;
        }
        if (ORDER_STATUS_PENDING_PAY.equals(order.getStatus())) {
            return ORDER_STATUS_PENDING_MATERIAL;
        }
        throw new BusinessException("当前订单状态无需审批");
    }

    private long safeCount(Long count) {
        return count == null ? 0L : count;
    }

    private int safeApprovalListLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_APPROVAL_LIST_LIMIT;
        }
        return Math.min(limit, MAX_APPROVAL_LIST_LIMIT);
    }

    private int safeAuditorOptionLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_AUDITOR_OPTION_LIMIT;
        }
        return Math.min(limit, MAX_AUDITOR_OPTION_LIMIT);
    }

    private String resolveAuditorPermissionCode(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        return switch (normalized) {
            case "leave" -> PermissionCatalogV3.CODE_APPROVAL_LEAVE_AUDIT;
            case "finance" -> PermissionCatalogV3.CODE_APPROVAL_FINANCE_AUDIT;
            case "resignation" -> PermissionCatalogV3.CODE_APPROVAL_RESIGNATION_AUDIT;
            case "order" -> PermissionCatalogV3.CODE_ORDER_AUDIT_SHIPMENT;
            case "quality" -> PermissionCatalogV3.CODE_QUALITY_AUDIT;
            default -> throw new BusinessException("审批类型不合法");
        };
    }

    private Long getManagerId(Long userId) {
        Employee employee = employeeMapper.selectById(userId);
        return employee == null ? null : employee.getManagerId();
    }

    private Integer getRoleLevel(Long userId) {
        Employee employee = employeeMapper.selectById(userId);
        return employee == null ? null : employee.getRoleLevel();
    }

    private String leaveTypeText(Integer leaveType) {
        return LeaveTypeEnum.of(leaveType).getLabel();
    }

    private String statusText(Integer status) {
        return ApprovalStatusEnum.of(status).getLabel();
    }

    private String qualityStatusText(String status) {
        return switch (StringUtils.hasText(status) ? status.trim() : "") {
            case BAD_PRODUCT_STATUS_PENDING_AUDIT -> "待质量审核";
            case BAD_PRODUCT_STATUS_PROCESSED -> "已通过";
            default -> "待处理";
        };
    }

    private String qualityTypeText(String type) {
        return switch (StringUtils.hasText(type) ? type.trim() : "") {
            case "raw_material" -> "原材料";
            case "process_standard" -> "工艺标准";
            case "process_flow" -> "工艺流程";
            case "motor" -> "电机";
            case "manual_track" -> "手动轨道";
            case "electric_track" -> "电动轨道";
            case "fabric" -> "面料";
            case "electric_roller_blind" -> "电动卷帘";
            case "manual_roller_blind" -> "手动卷帘";
            case "wear_part" -> "易损件";
            case "craft" -> "工艺";
            case "installation" -> "安装";
            case "measurement" -> "测量";
            case "after_sales_other", "other" -> "其他";
            default -> StringUtils.hasText(type) ? type.trim() : "质量记录";
        };
    }

    private String statusLabel(String status) {
        return switch (StringUtils.hasText(status) ? status.trim() : "") {
            case ORDER_STATUS_PENDING_CONFIRM -> "待确认";
            case ORDER_STATUS_PENDING_PAY -> "待收款";
            case ORDER_STATUS_PENDING_MATERIAL -> "备料中";
            case "budgeting" -> "预算中";
            case "budget_completed" -> "预算完成";
            case "producing" -> "生产中";
            case "pending_ship" -> "待发货";
            case "shipped" -> "已发货";
            case "completed" -> "已完成";
            case ORDER_STATUS_PENDING_CANCEL -> "取消审核中";
            case "cancelled" -> "已取消";
            default -> StringUtils.hasText(status) ? status.trim() : "未设置";
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeAttachmentName(String attachmentName, String attachmentUrl) {
        if (!StringUtils.hasText(attachmentUrl)) {
            return null;
        }
        String normalized = trimToNull(attachmentName);
        if (normalized == null) {
            return "finance-attachment";
        }
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }

    private Long normalizeAttachmentSize(Long attachmentSize, String attachmentUrl) {
        if (!StringUtils.hasText(attachmentUrl)) {
            return null;
        }
        if (attachmentSize == null || attachmentSize < 0) {
            return null;
        }
        return attachmentSize;
    }

    private String resolveContextPath() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }
        return contextPath.trim();
    }

}
