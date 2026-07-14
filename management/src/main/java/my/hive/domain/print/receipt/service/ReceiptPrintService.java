package my.hive.domain.print.receipt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.shared.context.OperationLogSkipContext;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.domain.print.PrintTaskService;
import my.hive.domain.print.PrintTaskStatus;
import my.hive.domain.print.dto.PrintTaskReportRequest;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import my.hive.domain.print.receipt.mapper.OutboundItemMapper;
import my.hive.domain.print.receipt.mapper.OutboundOrderMapper;
import my.hive.domain.print.receipt.mapper.OutboundPrintEditLogMapper;
import my.hive.domain.print.receipt.model.dto.OutboundPrintItemUpdateRequest;
import my.hive.domain.print.receipt.model.dto.OutboundPrintUpdateRequest;
import my.hive.domain.print.receipt.model.entity.OutboundItem;
import my.hive.domain.print.receipt.model.entity.OutboundOrder;
import my.hive.domain.print.receipt.model.entity.OutboundPrintEditLog;
import my.hive.domain.print.receipt.model.vo.OutboundPrintCommandVO;
import my.hive.domain.print.receipt.model.vo.OutboundPrintDetailVO;
import my.hive.domain.print.receipt.model.vo.OutboundPrintItemVO;
import my.hive.domain.print.receipt.model.vo.OutboundPrintOrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
/**
 * ReceiptPrintService 属于管理端后端打印回执模块，实现核心业务编排与规则逻辑。
 */
@Service
public class ReceiptPrintService {

    private static final int MAX_PRINT_ITEMS = 200;
    private static final BigDecimal MAX_DECIMAL_10_2 = new BigDecimal("99999999.99");
    private static final BigDecimal MAX_DECIMAL_12_2 = new BigDecimal("9999999999.99");

    @Resource
    private OutboundOrderMapper outboundOrderMapper;

    @Resource
    private OutboundItemMapper outboundItemMapper;

    @Resource
    private OutboundPrintEditLogMapper outboundPrintEditLogMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private PrintTaskService printTaskService;

    @Resource
    private ObjectMapper objectMapper;

    public List<OutboundPrintOrderVO> pendingList() {
        return outboundOrderMapper.selectPendingPrintList(TenantPermissionContext.getTenantCode());
    }

    public OutboundPrintDetailVO detail(String orderNo) {
        OutboundOrder order = requireOrder(orderNo);
        List<OutboundItem> items = outboundItemMapper.selectList(new LambdaQueryWrapper<OutboundItem>()
                .eq(OutboundItem::getOrderId, order.getId())
                .orderByAsc(OutboundItem::getId));

        OutboundPrintDetailVO detail = new OutboundPrintDetailVO();
        detail.setId(order.getId());
        detail.setOrderNo(order.getOrderNo());
        detail.setCustomerName(order.getCustomerName());
        detail.setProjectName(order.getProjectName());
        detail.setCreateTime(order.getCreateTime());
        detail.setPrintDate(order.getPrintDate());
        detail.setLogisticsCompany(order.getLogisticsCompany());
        detail.setLogisticsNo(order.getLogisticsNo());
        detail.setPrintEditCount(order.getPrintEditCount());
        detail.setOperator(StringUtils.hasText(order.getPrintOperatorName()) ? order.getPrintOperatorName() : resolveOperatorName(order.getOperatorId()));
        detail.setItems(items.stream().map(this::toItemVO).toList());
        detail.setTotalMeters((float) items.stream().mapToDouble(item -> item.getMeters() == null ? 0D : item.getMeters()).sum());
        detail.setTotalAmount(items.stream()
                .map(item -> item.getTotalAmount() == null ? BigDecimal.ZERO : item.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return detail;
    }

    public OutboundPrintCommandVO rawCommand(String orderNo) {
        OutboundPrintDetailVO detail = detail(orderNo);
        byte[] commandBytes = ReceiptPrinterCommandUtil.buildTriplicateCommand(detail);
        OutboundPrintCommandVO command = new OutboundPrintCommandVO();
        command.setOrderNo(detail.getOrderNo());
        command.setFileName(detail.getOrderNo() + ".prn");
        command.setDriverType("ESC_P");
        command.setContentType("application/octet-stream");
        command.setCharset(ReceiptPrinterCommandUtil.PRINT_CHARSET.name());
        command.setBase64Content(Base64.getEncoder().encodeToString(commandBytes));
        return command;
    }

    @Transactional(rollbackFor = Exception.class)
    public OutboundPrintDetailVO updatePrintDetail(OutboundPrintUpdateRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException("出库单ID不能为空");
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        OutboundOrder order = outboundOrderMapper.selectOne(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getId, request.getId())
                .eq(OutboundOrder::getOrderStatus, 1)
                .eq(OutboundOrder::getPrintStatus, 0)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("出库单不存在或不是待打印状态");
        }

        if (Boolean.TRUE.equals(request.getTimeCorrectionOnly())) {
            LocalDate printDate = request.getPrintDate();
            if (printDate == null) {
                throw new BusinessException("录单日期不能为空");
            }
            if (printDate.isAfter(LocalDate.now())) {
                throw new BusinessException("录单日期不能晚于当前日期");
            }
            order.setPrintDate(printDate);
            outboundOrderMapper.updateById(order);
            OperationLogSkipContext.skipCurrent();
            return detail(order.getOrderNo());
        }

        OutboundPrintDetailVO before = detail(order.getOrderNo());
        String nextOrderNo = requireText("单据编号", request.getOrderNo(), 64);
        ensureOrderNoAvailable(tenantCode, order.getId(), nextOrderNo);

        List<OutboundPrintItemUpdateRequest> itemRequests = normalizeItemRequests(request.getItems());
        order.setOrderNo(nextOrderNo);
        order.setCustomerName(optionalText("客户名称", request.getCustomerName(), 128));
        order.setProjectName(optionalText("项目名称", request.getProjectName(), 128));
        order.setPrintDate(request.getPrintDate() == null ? LocalDate.now() : request.getPrintDate());
        order.setPrintOperatorName(optionalText("制单人", request.getOperator(), 64));
        order.setLogisticsCompany(optionalText("物流公司", request.getLogisticsCompany(), 64));
        order.setLogisticsNo(optionalText("物流单号", request.getLogisticsNo(), 128));
        order.setPrintEditCount((order.getPrintEditCount() == null ? 0 : order.getPrintEditCount()) + 1);
        order.setUpdateTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);

        savePrintItems(tenantCode, order.getId(), itemRequests);
        OutboundPrintDetailVO after = detail(order.getOrderNo());
        insertEditLog(tenantCode, order.getId(), before.getOrderNo(), after.getOrderNo(), before, after, request.getEditReason());
        return after;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPrinted(String orderNo) {
        OutboundPrintDetailVO detail = detail(orderNo);
        updateStatus(orderNo, 2, 1, "出库单不存在或不是待打印状态");
        String taskNo = printTaskService.createReceiptTask(detail.getOrderNo(), detail, null, null, "网页端确认出库单已打印");
        if (taskNo != null) {
            PrintTaskReportRequest request = new PrintTaskReportRequest();
            request.setTaskNo(taskNo);
            request.setStatus(PrintTaskStatus.SUCCESS);
            request.setPrintChannel("browser");
            printTaskService.report(request);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo) {
        updateStatus(orderNo, 3, 0, "出库单不存在或当前不可作废");
    }

    private void updateStatus(String orderNo, Integer orderStatus, Integer printStatus, String errorMsg) {
        LambdaUpdateWrapper<OutboundOrder> wrapper = new LambdaUpdateWrapper<OutboundOrder>()
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
                .eq(OutboundOrder::getOrderNo, orderNo)
                .eq(OutboundOrder::getOrderStatus, 1)
                .eq(OutboundOrder::getPrintStatus, 0)
                .last("LIMIT 1"));
        if (order == null || !Objects.equals(order.getTenantCode(), TenantPermissionContext.getTenantCode())) {
            throw new BusinessException("出库单不存在或不是待打印状态");
        }
        return order;
    }

    private void ensureOrderNoAvailable(String tenantCode, Long currentOrderId, String orderNo) {
        Long count = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getOrderNo, orderNo)
                .ne(OutboundOrder::getId, currentOrderId));
        if (count != null && count > 0) {
            throw new BusinessException("单据编号已存在，不能重复");
        }
    }

    private List<OutboundPrintItemUpdateRequest> normalizeItemRequests(List<OutboundPrintItemUpdateRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("出库单至少需要一条明细");
        }
        if (items.size() > MAX_PRINT_ITEMS) {
            throw new BusinessException("单次最多保存 " + MAX_PRINT_ITEMS + " 条打印明细");
        }
        List<OutboundPrintItemUpdateRequest> normalizedItems = new ArrayList<>(items.size());
        Set<Long> itemIds = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            OutboundPrintItemUpdateRequest source = items.get(i);
            if (source == null) {
                throw new BusinessException("第 " + (i + 1) + " 行明细不能为空");
            }
            if (source.getId() != null && source.getId() <= 0) {
                throw new BusinessException("第 " + (i + 1) + " 行明细ID无效");
            }
            if (source.getId() != null && !itemIds.add(source.getId())) {
                throw new BusinessException("第 " + (i + 1) + " 行明细重复");
            }
            OutboundPrintItemUpdateRequest item = new OutboundPrintItemUpdateRequest();
            item.setId(source.getId());
            item.setBarcode(optionalText("第 " + (i + 1) + " 行条码", source.getBarcode(), 128));
            item.setModelCode(requireText("第 " + (i + 1) + " 行货物名称", source.getModelCode(), 128));
            item.setSpec(normalizeDecimal("第 " + (i + 1) + " 行规格", source.getSpec(), false, MAX_DECIMAL_10_2));
            item.setMeters(normalizeDecimal("第 " + (i + 1) + " 行米数", source.getMeters(), true, MAX_DECIMAL_10_2));
            item.setPrice(normalizeDecimal("第 " + (i + 1) + " 行单价", source.getPrice(), false, MAX_DECIMAL_12_2));
            BigDecimal totalAmount = normalizeDecimal("第 " + (i + 1) + " 行金额", source.getTotalAmount(), false, MAX_DECIMAL_12_2);
            if (totalAmount == null && item.getMeters() != null && item.getPrice() != null) {
                totalAmount = item.getMeters().multiply(item.getPrice()).setScale(2, RoundingMode.HALF_UP);
            }
            item.setTotalAmount(totalAmount);
            item.setRemark(optionalText("第 " + (i + 1) + " 行备注", source.getRemark(), 255));
            normalizedItems.add(item);
        }
        return normalizedItems;
    }

    private void savePrintItems(String tenantCode, Long orderId, List<OutboundPrintItemUpdateRequest> itemRequests) {
        List<OutboundItem> currentItems = outboundItemMapper.selectList(new LambdaQueryWrapper<OutboundItem>()
                .eq(OutboundItem::getOrderId, orderId)
                .orderByAsc(OutboundItem::getId));
        Map<Long, OutboundItem> currentItemMap = new HashMap<>();
        for (OutboundItem item : currentItems) {
            currentItemMap.put(item.getId(), item);
        }

        Set<Long> keptIds = new HashSet<>();
        for (int i = 0; i < itemRequests.size(); i++) {
            OutboundPrintItemUpdateRequest source = itemRequests.get(i);
            OutboundItem item;
            if (source.getId() == null) {
                item = new OutboundItem();
                item.setTenantCode(tenantCode);
                item.setOrderId(orderId);
                item.setRequestId("MANUAL-PRINT-" + UUID.randomUUID());
            } else {
                item = currentItemMap.get(source.getId());
                if (item == null) {
                    throw new BusinessException("第 " + (i + 1) + " 行明细不存在或不属于当前出库单");
                }
                keptIds.add(item.getId());
            }
            item.setBarcode(StringUtils.hasText(source.getBarcode()) ? source.getBarcode() : buildManualBarcode(orderId, i));
            item.setModelCode(source.getModelCode());
            item.setSpec(toFloat(source.getSpec()));
            item.setMeters(toFloat(source.getMeters()));
            item.setPrice(source.getPrice());
            item.setTotalAmount(source.getTotalAmount());
            item.setRemark(source.getRemark());
            if (item.getId() == null) {
                outboundItemMapper.insert(item);
            } else {
                outboundItemMapper.updateById(item);
            }
        }

        for (OutboundItem item : currentItems) {
            if (!keptIds.contains(item.getId())) {
                outboundItemMapper.delete(new LambdaQueryWrapper<OutboundItem>()
                        .eq(OutboundItem::getOrderId, orderId)
                        .eq(OutboundItem::getId, item.getId()));
            }
        }
    }

    private void insertEditLog(String tenantCode,
                               Long orderId,
                               String orderNoBefore,
                               String orderNoAfter,
                               OutboundPrintDetailVO before,
                               OutboundPrintDetailVO after,
                               String editReason) {
        OutboundPrintEditLog log = new OutboundPrintEditLog();
        log.setTenantCode(tenantCode);
        log.setOrderId(orderId);
        log.setOrderNoBefore(orderNoBefore);
        log.setOrderNoAfter(orderNoAfter);
        log.setOperatorUserId(TenantPermissionContext.getUserId());
        log.setBeforeJson(writeJson(before));
        log.setAfterJson(writeJson(after));
        log.setEditReason(optionalText("修正原因", editReason, 255));
        outboundPrintEditLogMapper.insert(log);
    }

    private String requireText(String fieldName, String value, int maxLength) {
        String normalized = optionalText(fieldName, value, maxLength);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String optionalText(String fieldName, String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private BigDecimal normalizeDecimal(String fieldName, BigDecimal value, boolean required, BigDecimal maxValue) {
        if (value == null) {
            if (required) {
                throw new BusinessException(fieldName + "不能为空");
            }
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(fieldName + "不能为负数");
        }
        if (value.compareTo(maxValue) > 0) {
            throw new BusinessException(fieldName + "超过允许范围");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Float toFloat(BigDecimal value) {
        return value == null ? null : value.floatValue();
    }

    private String buildManualBarcode(Long orderId, int index) {
        return "MANUAL-" + orderId + "-" + System.currentTimeMillis() + "-" + index;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("保存出库单修改记录失败");
        }
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
