package my.management.module.receipt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import my.management.module.receipt.mapper.OutboundItemMapper;
import my.management.module.receipt.mapper.OutboundOrderMapper;
import my.management.module.receipt.model.entity.OutboundItem;
import my.management.module.receipt.model.entity.OutboundOrder;
import my.management.module.receipt.model.vo.OutboundPrintDetailVO;
import my.management.module.receipt.model.vo.OutboundPrintItemVO;
import my.management.module.receipt.model.vo.OutboundPrintOrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ReceiptPrintService {

    @Resource
    private OutboundOrderMapper outboundOrderMapper;

    @Resource
    private OutboundItemMapper outboundItemMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    public List<OutboundPrintOrderVO> pendingList() {
        return outboundOrderMapper.selectPendingPrintList(TenantPermissionContext.getTenantCode());
    }

    public OutboundPrintDetailVO detail(String orderNo) {
        OutboundOrder order = requireOrder(orderNo);
        List<OutboundItem> items = outboundItemMapper.selectList(new LambdaQueryWrapper<OutboundItem>()
                .eq(OutboundItem::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(OutboundItem::getOrderId, order.getId())
                .orderByAsc(OutboundItem::getId));

        OutboundPrintDetailVO detail = new OutboundPrintDetailVO();
        detail.setId(order.getId());
        detail.setOrderNo(order.getOrderNo());
        detail.setCustomerName(order.getCustomerName());
        detail.setCreateTime(order.getCreateTime());
        detail.setOperator(resolveOperatorName(order.getOperatorId()));
        detail.setItems(items.stream().map(this::toItemVO).toList());
        detail.setTotalMeters((float) items.stream().mapToDouble(item -> item.getMeters() == null ? 0D : item.getMeters()).sum());
        detail.setTotalAmount(items.stream()
                .map(item -> item.getTotalAmount() == null ? BigDecimal.ZERO : item.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPrinted(String orderNo) {
        updateStatus(orderNo, 2, 1, "出库单不存在或不是待打印状态");
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo) {
        updateStatus(orderNo, 3, 0, "出库单不存在或当前不可作废");
    }

    private void updateStatus(String orderNo, Integer orderStatus, Integer printStatus, String errorMsg) {
        LambdaUpdateWrapper<OutboundOrder> wrapper = new LambdaUpdateWrapper<OutboundOrder>()
                .eq(OutboundOrder::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(OutboundOrder::getOrderNo, orderNo)
                .eq(OutboundOrder::getOrderStatus, 1)
                .eq(OutboundOrder::getPrintStatus, 0)
                .set(OutboundOrder::getOrderStatus, orderStatus)
                .set(OutboundOrder::getPrintStatus, printStatus)
                .set(OutboundOrder::getUpdateTime, LocalDateTime.now());
        int rows = outboundOrderMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException(errorMsg);
        }
    }

    private OutboundOrder requireOrder(String orderNo) {
        OutboundOrder order = outboundOrderMapper.selectOne(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(OutboundOrder::getOrderNo, orderNo)
                .eq(OutboundOrder::getOrderStatus, 1)
                .eq(OutboundOrder::getPrintStatus, 0)
                .last("LIMIT 1"));
        if (order == null || !Objects.equals(order.getTenantCode(), TenantPermissionContext.getTenantCode())) {
            throw new BusinessException("出库单不存在或不是待打印状态");
        }
        return order;
    }

    private OutboundPrintItemVO toItemVO(OutboundItem item) {
        OutboundPrintItemVO vo = new OutboundPrintItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private String resolveOperatorName(Long operatorId) {
        if (operatorId == null) {
            return "--";
        }
        Employee operator = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(Employee::getId, operatorId)
                .last("LIMIT 1"));
        if (operator == null || operator.getName() == null || operator.getName().isBlank()) {
            return "用户" + operatorId;
        }
        return operator.getName();
    }
}
