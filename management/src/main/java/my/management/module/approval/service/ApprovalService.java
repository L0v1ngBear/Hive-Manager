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
import my.management.module.approval.model.vo.FinanceApprovalVO;
import my.management.module.approval.model.vo.LeaveApprovalListVO;
import my.management.module.approval.model.vo.LeaveDetailVO;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * ApprovalService 属于管理端后端审批模块，实现核心业务编排与规则逻辑。
 */
@Service
public class ApprovalService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int ACTION_APPROVE = 1;

    @Resource
    private LeaveMapper leaveMapper;

    @Resource
    private FinanceApprovalMapper financeApprovalMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

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
        if (!STATUS_PENDING_EQUALS(userLeave.getStatus())) {
            throw new BusinessException("该请假单已处理，请勿重复审批");
        }
        userLeave.setAuditComment(trimToNull(request.getComment()));
        if (ACTION_APPROVE == request.getAction()) {
            Long nextManagerId = getManagerId(currentUserId);
            Integer roleLevel = getRoleLevel(currentUserId);
            if ((roleLevel != null && roleLevel >= 2) || nextManagerId == null) {
                userLeave.setStatus(STATUS_APPROVED);
            } else {
                userLeave.setAuditorId(nextManagerId);
            }
        } else {
            userLeave.setStatus(STATUS_REJECTED);
        }
        leaveMapper.updateById(userLeave);
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
        approval.setStatus(STATUS_PENDING);
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
        if (!STATUS_PENDING_EQUALS(approval.getStatus())) {
            throw new BusinessException("该财务审批单已处理，请勿重复审批");
        }

        approval.setAuditComment(trimToNull(request.getComment()));
        if (ACTION_APPROVE == request.getAction()) {
            Long nextManagerId = getManagerId(currentUserId);
            Integer roleLevel = getRoleLevel(currentUserId);
            if ((roleLevel != null && roleLevel >= 2) || nextManagerId == null) {
                approval.setStatus(STATUS_APPROVED);
            } else {
                approval.setAuditorId(nextManagerId);
            }
        } else {
            approval.setStatus(STATUS_REJECTED);
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

    private boolean STATUS_PENDING_EQUALS(Integer status) {
        return status != null && status == STATUS_PENDING;
    }

    private String leaveTypeText(Integer leaveType) {
        if (leaveType == null) {
            return "未填写";
        }
        return switch (leaveType) {
            case 1 -> "事假";
            case 2 -> "病假";
            case 3 -> "年假";
            case 4 -> "调休";
            default -> "其他";
        };
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_PENDING -> "待审批";
            case STATUS_APPROVED -> "已通过";
            case STATUS_REJECTED -> "已拒绝";
            default -> "未知";
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
