package my.management.module.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.security.InternalUploadUrlValidator;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.approval.mapper.FinanceApprovalMapper;
import my.management.module.approval.mapper.LeaveMapper;
import my.management.module.approval.mapper.ResignationApprovalMapper;
import my.management.module.approval.model.dto.FinanceAuditRequest;
import my.management.module.approval.model.dto.FinanceSubmitRequest;
import my.management.module.approval.model.dto.LeaveAuditRequest;
import my.management.module.approval.model.dto.OrderApprovalAuditRequest;
import my.management.module.approval.model.dto.ResignationAuditRequest;
import my.management.module.approval.model.dto.ResignationSubmitRequest;
import my.management.module.approval.model.entity.FinanceApproval;
import my.management.module.approval.model.entity.ResignationApproval;
import my.management.module.approval.model.entity.UserLeave;
import my.management.module.approval.model.enums.ApprovalActionEnum;
import my.management.module.approval.model.enums.ApprovalStatusEnum;
import my.management.module.approval.model.enums.LeaveTypeEnum;
import my.management.module.approval.model.vo.ApprovalSummaryVO;
import my.management.module.approval.model.vo.FinanceApprovalVO;
import my.management.module.approval.model.vo.LeaveApprovalListVO;
import my.management.module.approval.model.vo.LeaveDetailVO;
import my.management.module.approval.model.vo.OrderApprovalVO;
import my.management.module.approval.model.vo.ResignationApprovalVO;
import my.management.module.attendance.mapper.AttendanceRecordMapper;
import my.management.module.attendance.mapper.TenantAttendanceRuleManageMapper;
import my.management.module.attendance.model.entity.AttendanceRecord;
import my.management.module.attendance.model.entity.TenantAttendanceRule;
import my.management.module.attendance.model.enums.AttendancePunchStatusEnum;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import my.management.module.employee.service.EmployeeService;
import my.management.module.order.mapper.ProductionOrderMapper;
import my.management.module.order.mapper.SalesOrderMapper;
import my.management.module.order.model.dto.ProductionOrderUpdateRequest;
import my.management.module.order.model.dto.SalesOrderUpdateRequest;
import my.management.module.order.model.entity.ProductionOrder;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.service.OrderService;
import my.management.module.sys.model.enums.PermissionCodeEnum;
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
import java.util.List;
/**
 * ApprovalService 属于管理端后端审批模块，实现核心业务编排与规则逻辑。
 */
@Service
public class ApprovalService {

    private static final String ORDER_TYPE_SALES = "sales";
    private static final String ORDER_TYPE_PRODUCTION = "production";
    private static final String ORDER_STATUS_PENDING_CONFIRM = "pending_confirm";
    private static final String ORDER_STATUS_PENDING_PAY = "pending_pay";
    private static final String ORDER_STATUS_PRODUCING = "producing";
    private static final String SALES_APPROVED_NEXT_STATUS = "pending_pay";
    private static final String PRODUCTION_APPROVED_NEXT_STATUS = "pending_material";
    private static final String APPROVAL_TYPE_LEAVE = "LEAVE";
    private static final String APPROVAL_TYPE_FINANCE = "FINANCE";
    private static final String APPROVAL_TYPE_RESIGNATION = "RESIGNATION";
    private static final int MAX_PARALLEL_APPROVERS = 30;

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
    private ApprovalAuditorCandidateService approvalAuditorCandidateService;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public ApprovalSummaryVO getSummary() {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();

        LambdaQueryWrapper<UserLeave> leavePendingWrapper = new LambdaQueryWrapper<UserLeave>()
                .eq(UserLeave::getTenantCode, tenantCode)
                .eq(UserLeave::getStatus, ApprovalStatusEnum.PENDING.getCode());
        appendLeaveAuditorFilter(leavePendingWrapper, userId);
        long leavePending = safeCount(leaveMapper.selectCount(leavePendingWrapper));

        LambdaQueryWrapper<FinanceApproval> financePendingWrapper = new LambdaQueryWrapper<FinanceApproval>()
                .eq(FinanceApproval::getTenantCode, tenantCode)
                .eq(FinanceApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
        appendFinanceAuditorFilter(financePendingWrapper, userId);
        long financePending = safeCount(financeApprovalMapper.selectCount(financePendingWrapper));

        LambdaQueryWrapper<ResignationApproval> resignationPendingWrapper = new LambdaQueryWrapper<ResignationApproval>()
                .eq(ResignationApproval::getTenantCode, tenantCode)
                .eq(ResignationApproval::getStatus, ApprovalStatusEnum.PENDING.getCode());
        appendResignationAuditorFilter(resignationPendingWrapper, userId);
        long resignationPending = safeCount(resignationApprovalMapper.selectCount(resignationPendingWrapper));
        long orderPending = countPendingOrderApprovals(tenantCode);

        ApprovalSummaryVO vo = new ApprovalSummaryVO();
        vo.setLeavePending(leavePending);
        vo.setFinancePending(financePending);
        vo.setResignationPending(resignationPending);
        vo.setOrderPending(orderPending);
        vo.setTotalPending(leavePending + financePending + resignationPending + orderPending);
        return vo;
    }

    public List<LeaveApprovalListVO> listLeaveApprovals() {
        Long userId = TenantPermissionContext.getUserId();
        LambdaQueryWrapper<UserLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(UserLeave::getApplyUserId, userId)
                        .or()
                        .eq(UserLeave::getAuditorId, userId)
                        .or()
                        .apply("FIND_IN_SET({0}, auditor_ids) > 0", String.valueOf(userId)));
        wrapper.orderByDesc(UserLeave::getCreateTime);
        return leaveMapper.selectList(wrapper).stream().map(this::toLeaveListVO).toList();
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
        userLeave.setAuditComment(trimToNull(request.getComment()));
        if (ApprovalActionEnum.isApprove(request.getAction())) {
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
        LocalDate currentDate = leave.getStartTime().toLocalDate();
        LocalDate endDate = leave.getEndTime().toLocalDate();
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

    public List<FinanceApprovalVO> listFinanceApprovals() {
        Long userId = TenantPermissionContext.getUserId();
        LambdaQueryWrapper<FinanceApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(FinanceApproval::getApplyUserId, userId)
                        .or()
                        .eq(FinanceApproval::getAuditorId, userId)
                        .or()
                        .apply("FIND_IN_SET({0}, auditor_ids) > 0", String.valueOf(userId)));
        wrapper.orderByDesc(FinanceApproval::getCreateTime);
        return financeApprovalMapper.selectList(wrapper).stream().map(this::toFinanceVO).toList();
    }

    public FinanceApprovalVO getFinanceDetail(String approvalCode) {
        return toFinanceVO(getFinanceByCode(approvalCode));
    }

    public List<ResignationApprovalVO> listResignationApprovals() {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();
        LambdaQueryWrapper<ResignationApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResignationApproval::getTenantCode, tenantCode);
        wrapper.and(q -> q.eq(ResignationApproval::getApplyUserId, userId)
                .or()
                .eq(ResignationApproval::getAuditorId, userId)
                .or()
                .apply("FIND_IN_SET({0}, auditor_ids) > 0", String.valueOf(userId)));
        wrapper.orderByDesc(ResignationApproval::getCreateTime);
        return resignationApprovalMapper.selectList(wrapper).stream().map(this::toResignationVO).toList();
    }

    public ResignationApprovalVO getResignationDetail(String resignationCode) {
        return toResignationVO(getResignationByCode(resignationCode));
    }

    public List<OrderApprovalVO> listOrderApprovals() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        List<OrderApprovalVO> salesRows = salesOrderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getTenantCode, tenantCode)
                        .in(SalesOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM, ORDER_STATUS_PENDING_PAY)
                        .orderByDesc(SalesOrder::getCreateTime))
                .stream()
                .map(this::toSalesOrderApprovalVO)
                .toList();
        List<OrderApprovalVO> productionRows = productionOrderMapper.selectList(new LambdaQueryWrapper<ProductionOrder>()
                        .eq(ProductionOrder::getTenantCode, tenantCode)
                        .eq(ProductionOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM)
                        .orderByDesc(ProductionOrder::getCreateTime))
                .stream()
                .map(this::toProductionOrderApprovalVO)
                .toList();

        return java.util.stream.Stream.concat(salesRows.stream(), productionRows.stream())
                .sorted(Comparator.comparing(OrderApprovalVO::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public OrderApprovalVO getOrderApprovalDetail(String orderType, String orderId) {
        if (ORDER_TYPE_SALES.equalsIgnoreCase(orderType)) {
            return toSalesOrderApprovalVO(findPendingSalesOrder(orderId));
        }
        if (ORDER_TYPE_PRODUCTION.equalsIgnoreCase(orderType)) {
            return toProductionOrderApprovalVO(findPendingProductionOrder(orderId));
        }
        throw new BusinessException("订单审批类型不合法");
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditOrder(OrderApprovalAuditRequest request) {
        if (!ApprovalActionEnum.isApprove(request.getAction())) {
            throw new BusinessException("订单驳回/取消请到订单管理中处理，避免误改业务单据状态");
        }
        String orderType = request.getOrderType() == null ? "" : request.getOrderType().trim().toLowerCase();
        String remark = StringUtils.hasText(request.getComment()) ? request.getComment().trim() : "审批中心确认订单";

        if (ORDER_TYPE_SALES.equals(orderType)) {
            SalesOrder salesOrder = findPendingSalesOrder(request.getOrderId());
            SalesOrderUpdateRequest updateRequest = new SalesOrderUpdateRequest();
            updateRequest.setStatus(resolveSalesApprovalNextStatus(salesOrder));
            updateRequest.setRemark(remark);
            orderService.approveSalesOrderTransition(request.getOrderId(), updateRequest.getStatus(), updateRequest.getRemark());
            return;
        }
        if (ORDER_TYPE_PRODUCTION.equals(orderType)) {
            findPendingProductionOrder(request.getOrderId());
            ProductionOrderUpdateRequest updateRequest = new ProductionOrderUpdateRequest();
            updateRequest.setStatus(PRODUCTION_APPROVED_NEXT_STATUS);
            updateRequest.setRemark(remark);
            orderService.updateProductionOrder(request.getOrderId(), updateRequest);
            return;
        }
        throw new BusinessException("订单审批类型不合法");
    }

    @Transactional(rollbackFor = Exception.class)
    public String submitResignation(ResignationSubmitRequest request) {
        Long userId = TenantPermissionContext.getUserId();
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long managerId = getManagerId(userId);

        Long pendingCount = resignationApprovalMapper.selectCount(new LambdaQueryWrapper<ResignationApproval>()
                .eq(ResignationApproval::getTenantCode, tenantCode)
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
        assignResignationAuditors(approval, managerId);
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

        approval.setAuditComment(trimToNull(request.getComment()));
        if (ApprovalActionEnum.isApprove(request.getAction())) {
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
        Long managerId = getManagerId(userId);

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
        assignFinanceAuditors(approval, managerId);
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

        approval.setAuditComment(trimToNull(request.getComment()));
        if (ApprovalActionEnum.isApprove(request.getAction())) {
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
                .eq(ResignationApproval::getTenantCode, TenantPermissionContext.getTenantCode())
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

    private OrderApprovalVO toSalesOrderApprovalVO(SalesOrder order) {
        boolean payToProductionApproval = ORDER_STATUS_PENDING_PAY.equals(order.getStatus());
        OrderApprovalVO vo = new OrderApprovalVO();
        vo.setOrderType(ORDER_TYPE_SALES);
        vo.setOrderTypeText("销售订单");
        vo.setOrderId(order.getOrderId());
        vo.setCustomerName(order.getCustomerName());
        vo.setProjectName(order.getProjectName());
        String fallbackSummary = payToProductionApproval ? "待审批转生产中销售订单" : "待确认销售订单";
        vo.setSummary((StringUtils.hasText(order.getGoodsDesc()) ? order.getGoodsDesc() : fallbackSummary)
                + " / 金额 ￥" + (order.getTotalAmount() == null ? "0.00" : order.getTotalAmount()));
        vo.setStatus(order.getStatus());
        vo.setStatusText(payToProductionApproval ? "待审批转生产中" : "待确认");
        vo.setCreateTime(order.getCreateTime());
        vo.setCanAudit(Boolean.TRUE);
        return vo;
    }

    private OrderApprovalVO toProductionOrderApprovalVO(ProductionOrder order) {
        OrderApprovalVO vo = new OrderApprovalVO();
        vo.setOrderType(ORDER_TYPE_PRODUCTION);
        vo.setOrderTypeText("生产订单");
        vo.setOrderId(order.getOrderId());
        vo.setCustomerName(order.getCustomerName());
        vo.setProjectName(order.getProjectName());
        vo.setSummary((StringUtils.hasText(order.getModelCode()) ? order.getModelCode() : "待确认生产订单")
                + " / 数量 " + (order.getQuantity() == null ? 0 : order.getQuantity()));
        vo.setStatus(order.getStatus());
        vo.setStatusText("待确认");
        vo.setCreateTime(order.getCreateTime());
        vo.setCanAudit(Boolean.TRUE);
        return vo;
    }

    private SalesOrder findPendingSalesOrder(String orderId) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(SalesOrder::getOrderId, orderId)
                .in(SalesOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM, ORDER_STATUS_PENDING_PAY));
        if (order == null) {
            throw new BusinessException("待审批销售订单不存在或已处理");
        }
        return order;
    }

    private ProductionOrder findPendingProductionOrder(String orderId) {
        ProductionOrder order = productionOrderMapper.selectOne(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(ProductionOrder::getOrderId, orderId)
                .eq(ProductionOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM));
        if (order == null) {
            throw new BusinessException("待确认生产订单不存在或已处理");
        }
        return order;
    }

    private long countPendingOrderApprovals(String tenantCode) {
        long salesCount = safeCount(salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantCode, tenantCode)
                .in(SalesOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM, ORDER_STATUS_PENDING_PAY)));
        long productionCount = safeCount(productionOrderMapper.selectCount(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getTenantCode, tenantCode)
                .eq(ProductionOrder::getStatus, ORDER_STATUS_PENDING_CONFIRM)));
        return salesCount + productionCount;
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

    private void assignLeaveAuditors(UserLeave approval, Long primaryAuditorId) {
        List<Long> auditorIds = resolveParallelAuditorIds(
                approval.getTenantCode(),
                approval.getApplyUserId(),
                primaryAuditorId,
                PermissionCodeEnum.CODE_APPROVAL_LEAVE_AUDIT
        );
        applyAuditorIds(approval::setAuditorId, approval::setAuditorIds, auditorIds);
    }

    private void assignFinanceAuditors(FinanceApproval approval, Long primaryAuditorId) {
        List<Long> auditorIds = resolveParallelAuditorIds(
                approval.getTenantCode(),
                approval.getApplyUserId(),
                primaryAuditorId,
                PermissionCodeEnum.CODE_APPROVAL_FINANCE_AUDIT
        );
        applyAuditorIds(approval::setAuditorId, approval::setAuditorIds, auditorIds);
    }

    private void assignResignationAuditors(ResignationApproval approval, Long primaryAuditorId) {
        List<Long> auditorIds = resolveParallelAuditorIds(
                approval.getTenantCode(),
                approval.getApplyUserId(),
                primaryAuditorId,
                PermissionCodeEnum.CODE_APPROVAL_RESIGNATION_AUDIT
        );
        applyAuditorIds(approval::setAuditorId, approval::setAuditorIds, auditorIds);
    }

    private List<Long> resolveParallelAuditorIds(String tenantCode,
                                                 Long applyUserId,
                                                 Long primaryAuditorId,
                                                 String permissionCode) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        addCandidateAuditor(ids, primaryAuditorId, applyUserId);
        if (StringUtils.hasText(tenantCode) && StringUtils.hasText(permissionCode)) {
            List<Long> permissionAuditorIds = employeeMapper.selectActiveApproverIdsByPermission(tenantCode, permissionCode);
            if (permissionAuditorIds != null) {
                permissionAuditorIds.forEach(id -> addCandidateAuditor(ids, id, applyUserId));
            }
        }
        if (ids.isEmpty()) {
            throw new BusinessException("未找到可用审批人，请先配置直属负责人或审批角色权限");
        }
        return ids.stream().limit(MAX_PARALLEL_APPROVERS).toList();
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

    private void applyAuditorIds(java.util.function.Consumer<Long> primarySetter,
                                 java.util.function.Consumer<String> idsSetter,
                                 List<Long> auditorIds) {
        if (auditorIds == null || auditorIds.isEmpty()) {
            throw new BusinessException("未找到可用审批人，请先配置直属负责人或审批角色权限");
        }
        primarySetter.accept(auditorIds.get(0));
        idsSetter.accept(joinAuditorIds(auditorIds));
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
            return ORDER_STATUS_PRODUCING;
        }
        throw new BusinessException("当前销售订单状态无需审批");
    }

    private long safeCount(Long count) {
        return count == null ? 0L : count;
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
