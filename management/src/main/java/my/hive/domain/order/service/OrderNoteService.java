package my.hive.domain.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.order.mapper.SalesOrderNoteMapper;
import my.hive.domain.order.mapper.SalesOrderStatusLogMapper;
import my.hive.domain.order.model.dto.SalesOrderNoteSaveRequest;
import my.hive.domain.order.model.entity.SalesOrderNote;
import my.hive.domain.order.model.entity.SalesOrderStatusLog;
import my.hive.domain.order.model.enums.OrderLogOperateTypeEnum;
import my.hive.domain.order.model.vo.SalesOrderNoteVO;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.PermissionCatalogV3;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderNoteService {

    private static final int MAX_NOTES = 50;
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final SalesOrderNoteMapper noteMapper;
    private final SalesOrderStatusLogMapper logMapper;
    private final EmployeeMapper employeeMapper;

    public OrderNoteService(SalesOrderNoteMapper noteMapper,
                            SalesOrderStatusLogMapper logMapper,
                            EmployeeMapper employeeMapper) {
        this.noteMapper = noteMapper;
        this.logMapper = logMapper;
        this.employeeMapper = employeeMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<SalesOrderNoteVO> saveNotes(String tenantCode,
                                            String orderId,
                                            String orderStatus,
                                            List<SalesOrderNoteSaveRequest> requests) {
        List<SalesOrderNoteSaveRequest> safeRequests = requests == null ? Collections.emptyList() : requests;
        if (safeRequests.size() > MAX_NOTES) {
            throw new BusinessException("每个订单最多添加50条备注");
        }

        List<SalesOrderNote> existingNotes = selectNotes(tenantCode, orderId);
        Map<Long, SalesOrderNote> existingById = new LinkedHashMap<>();
        for (SalesOrderNote note : existingNotes) {
            existingById.put(note.getId(), note);
        }

        Long userId = TenantPermissionContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        String userName = resolveCurrentUserName(tenantCode, userId);
        LocalDateTime now = LocalDateTime.now();

        for (SalesOrderNoteSaveRequest request : safeRequests) {
            String content = normalizeContent(request == null ? null : request.getContent());
            if (request.getId() == null) {
                requirePermission(PermissionCatalogV3.CODE_ORDER_NOTE_CREATE, "无新增订单备注权限");
                SalesOrderNote note = new SalesOrderNote();
                note.setTenantCode(tenantCode);
                note.setOrderId(orderId);
                note.setContent(content);
                note.setCreatorUserId(userId);
                note.setCreatorName(userName);
                note.setUpdaterUserId(userId);
                note.setUpdaterName(userName);
                note.setVersion(0);
                note.setCreateTime(now);
                note.setUpdateTime(now);
                noteMapper.insert(note);
                insertOperationLog(tenantCode, orderId, orderStatus, note.getId(),
                        OrderLogOperateTypeEnum.NOTE_CREATE, userId, userName, now);
                continue;
            }

            SalesOrderNote existing = existingById.get(request.getId());
            if (existing == null) {
                throw new BusinessException("备注不存在或不属于当前订单");
            }
            if (request.getVersion() == null || !request.getVersion().equals(existing.getVersion())) {
                throw new BusinessException(409, "备注已被其他人修改，请刷新后重试");
            }
            if (content.equals(existing.getContent())) {
                continue;
            }
            requirePermission(PermissionCatalogV3.CODE_ORDER_NOTE_UPDATE, "无修改订单备注权限");
            int changed = noteMapper.updateContent(existing.getId(), tenantCode, orderId,
                    request.getVersion(), content, userId, userName, now);
            if (changed != 1) {
                throw new BusinessException(409, "备注已被其他人修改，请刷新后重试");
            }
            insertOperationLog(tenantCode, orderId, orderStatus, existing.getId(),
                    OrderLogOperateTypeEnum.NOTE_UPDATE, userId, userName, now);
        }
        return listNotesIfPermitted(tenantCode, orderId);
    }

    public List<SalesOrderNoteVO> listNotesIfPermitted(String tenantCode, String orderId) {
        if (!TenantPermissionContext.hasPermission(PermissionCatalogV3.CODE_ORDER_NOTE_VIEW)) {
            return Collections.emptyList();
        }
        return selectNotes(tenantCode, orderId).stream().map(this::toVO).toList();
    }

    private List<SalesOrderNote> selectNotes(String tenantCode, String orderId) {
        return noteMapper.selectList(new LambdaQueryWrapper<SalesOrderNote>()
                .eq(SalesOrderNote::getTenantCode, tenantCode)
                .eq(SalesOrderNote::getOrderId, orderId)
                .orderByDesc(SalesOrderNote::getUpdateTime)
                .orderByDesc(SalesOrderNote::getId));
    }

    private String normalizeContent(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("备注内容不能为空");
        }
        String content = value.trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("单条备注不能超过1000字");
        }
        return content;
    }

    private void requirePermission(String code, String message) {
        if (!TenantPermissionContext.hasPermission(code)) {
            throw new BusinessException(403, message);
        }
    }

    private String resolveCurrentUserName(String tenantCode, Long userId) {
        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, tenantCode)
                .eq(Employee::getId, userId)
                .last("LIMIT 1"));
        return employee != null && StringUtils.hasText(employee.getName())
                ? employee.getName().trim()
                : String.valueOf(userId);
    }

    private void insertOperationLog(String tenantCode,
                                    String orderId,
                                    String orderStatus,
                                    Long noteId,
                                    OrderLogOperateTypeEnum operateType,
                                    Long userId,
                                    String userName,
                                    LocalDateTime now) {
        SalesOrderStatusLog log = new SalesOrderStatusLog();
        log.setTenantCode(tenantCode);
        log.setOrderId(orderId);
        log.setOldStatus(orderStatus);
        log.setNewStatus(orderStatus);
        log.setOperateType(operateType.getCode());
        log.setRemark("订单备注记录 ID：" + noteId);
        log.setOperator(String.valueOf(userId));
        log.setOperatorName(userName);
        log.setCreateTime(now);
        logMapper.insert(log);
    }

    private SalesOrderNoteVO toVO(SalesOrderNote note) {
        SalesOrderNoteVO vo = new SalesOrderNoteVO();
        BeanUtils.copyProperties(note, vo);
        return vo;
    }
}
