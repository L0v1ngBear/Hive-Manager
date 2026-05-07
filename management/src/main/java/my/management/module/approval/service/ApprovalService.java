package my.management.module.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.approval.mapper.FinanceApprovalMapper;
import my.management.module.approval.mapper.LeaveMapper;
import my.management.module.approval.model.dto.FinanceAuditRequest;
import my.management.module.approval.model.dto.FinanceSubmitRequest;
import my.management.module.approval.model.dto.LeaveAuditRequest;
import my.management.module.approval.model.entity.FinanceApproval;
import my.management.module.approval.model.entity.UserLeave;
import my.management.module.approval.model.enums.ApprovalActionEnum;
import my.management.module.approval.model.enums.ApprovalStatusEnum;
import my.management.module.approval.model.enums.LeaveTypeEnum;
import my.management.module.approval.model.vo.FinanceApprovalVO;
import my.management.module.approval.model.vo.LeaveApprovalListVO;
import my.management.module.approval.model.vo.LeaveDetailVO;
import my.management.module.attendance.mapper.AttendanceRecordMapper;
import my.management.module.attendance.mapper.TenantAttendanceRuleManageMapper;
import my.management.module.attendance.model.entity.AttendanceRecord;
import my.management.module.attendance.model.entity.TenantAttendanceRule;
import my.management.module.attendance.model.enums.AttendancePunchStatusEnum;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private LeaveMapper leaveMapper;

    @Resource
    private FinanceApprovalMapper financeApprovalMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private AttendanceRecordMapper attendanceRecordMapper;

    @Resource
    private TenantAttendanceRuleManageMapper tenantAttendanceRuleManageMapper;

    public List<LeaveApprovalListVO> listLeaveApprovals() {
        Long userId = TenantPermissionContext.getUserId();
        LambdaQueryWrapper<UserLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(UserLeave::getApplyUserId, userId)
                        .or()
                        .eq(UserLeave::getAuditorId, userId));
        wrapper.orderByDesc(UserLeave::getCreateTime);
        return leaveMapper.selectList(wrapper).stream().map(this::toLeaveListVO).toList();
    }

    public LeaveDetailVO getLeaveDetail(String leaveCode) {
        UserLeave userLeave = getLeaveByCode(leaveCode);
        LeaveDetailVO vo = new LeaveDetailVO();
        BeanUtils.copyProperties(userLeave, vo);
        Employee applyUser = employeeMapper.selectById(userLeave.getApplyUserId());
        Employee auditor = userLeave.getAuditorId() == null ? null : employeeMapper.selectById(userLeave.getAuditorId());
        vo.setApplyUserName(applyUser == null ? "未知员工" : applyUser.getName());
        vo.setAuditorName(auditor == null ? "待分配" : auditor.getName());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditLeave(LeaveAuditRequest request) {
        Long currentUserId = TenantPermissionContext.getUserId();
        UserLeave userLeave = getLeaveByCode(request.getLeaveCode());
        if (userLeave.getAuditorId() == null || !currentUserId.equals(userLeave.getAuditorId())) {
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
                userLeave.setAuditorId(nextManagerId);
            }
        } else {
            userLeave.setStatus(ApprovalStatusEnum.REJECTED.getCode());
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
                        .eq(FinanceApproval::getAuditorId, userId));
        wrapper.orderByDesc(FinanceApproval::getCreateTime);
        return financeApprovalMapper.selectList(wrapper).stream().map(this::toFinanceVO).toList();
    }

    public FinanceApprovalVO getFinanceDetail(String approvalCode) {
        return toFinanceVO(getFinanceByCode(approvalCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public String submitFinance(FinanceSubmitRequest request) {
        Long userId = TenantPermissionContext.getUserId();
        Long managerId = getManagerId(userId);
        if (managerId == null) {
            throw new BusinessException("未找到直属审批人，请联系管理员配置组织架构");
        }

        FinanceApproval approval = new FinanceApproval();
        approval.setApprovalCode(codeGeneratorUtil.generateCode("FIN", 4));
        approval.setTenantCode(TenantPermissionContext.getTenantCode());
        approval.setApplyUserId(userId);
        approval.setCategory(request.getCategory().trim());
        approval.setAmount(request.getAmount());
        approval.setReason(request.getReason().trim());
        approval.setAttachmentUrl(trimToNull(request.getAttachmentUrl()));
        approval.setStatus(ApprovalStatusEnum.PENDING.getCode());
        approval.setAuditorId(managerId);
        financeApprovalMapper.insert(approval);
        return approval.getApprovalCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditFinance(FinanceAuditRequest request) {
        Long currentUserId = TenantPermissionContext.getUserId();
        FinanceApproval approval = getFinanceByCode(request.getApprovalCode());
        if (approval.getAuditorId() == null || !currentUserId.equals(approval.getAuditorId())) {
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
                approval.setAuditorId(nextManagerId);
            }
        } else {
            approval.setStatus(ApprovalStatusEnum.REJECTED.getCode());
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
        if (leave.getAuditorId() != null) {
            Employee auditor = employeeMapper.selectById(leave.getAuditorId());
            if (auditor != null) {
                vo.setAuditorName(auditor.getName());
            }
        }
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
        if (approval.getAuditorId() != null) {
            Employee auditor = employeeMapper.selectById(approval.getAuditorId());
            if (auditor != null) {
                vo.setAuditorName(auditor.getName());
            }
        }
        return vo;
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

}
