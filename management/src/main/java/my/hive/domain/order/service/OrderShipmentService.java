package my.hive.domain.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.order.mapper.SalesOrderShipmentMapper;
import my.hive.domain.order.model.dto.SalesOrderShipmentSaveRequest;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import my.hive.domain.order.model.vo.SalesOrderShipmentVO;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.external.ExternalApiGuardService;
import my.hive.shared.log.OperationLogCollector;
import my.hive.shared.log.OperationLogEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderShipmentService {

    private static final int MAX_SHIPMENTS = 50;

    private final SalesOrderShipmentMapper shipmentMapper;
    private final EmployeeMapper employeeMapper;
    private final OperationLogCollector operationLogCollector;
    private final ExternalApiGuardService externalApiGuardService;

    public OrderShipmentService(SalesOrderShipmentMapper shipmentMapper,
                                EmployeeMapper employeeMapper,
                                OperationLogCollector operationLogCollector,
                                ExternalApiGuardService externalApiGuardService) {
        this.shipmentMapper = shipmentMapper;
        this.employeeMapper = employeeMapper;
        this.operationLogCollector = operationLogCollector;
        this.externalApiGuardService = externalApiGuardService;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<SalesOrderShipmentVO> saveShipments(String tenantCode,
                                                     String orderId,
                                                     List<SalesOrderShipmentSaveRequest> requests) {
        List<NormalizedShipment> normalized = normalizeRequests(requests);
        List<SalesOrderShipment> existingShipments = selectShipments(tenantCode, orderId);
        Map<Long, SalesOrderShipment> existingById = new LinkedHashMap<>();
        for (SalesOrderShipment shipment : existingShipments) {
            existingById.put(shipment.getId(), shipment);
        }

        Set<Long> submittedExistingIds = new LinkedHashSet<>();
        for (NormalizedShipment shipment : normalized) {
            if (shipment.id() == null) {
                continue;
            }
            if (!existingById.containsKey(shipment.id())) {
                throw new BusinessException("Shipment does not exist or does not belong to this order");
            }
            if (!submittedExistingIds.add(shipment.id())) {
                throw new BusinessException("Duplicate shipment id");
            }
        }
        if (!submittedExistingIds.equals(existingById.keySet())) {
            throw new BusinessException("已保存的物流记录不允许删除");
        }

        Long userId = requireCurrentUserId();
        String userName = resolveCurrentUserName(tenantCode, userId);
        String user = String.valueOf(userId);
        LocalDateTime now = LocalDateTime.now();
        List<ShipmentEvent> pendingEvents = new ArrayList<>();
        for (int index = 0; index < normalized.size(); index++) {
            NormalizedShipment request = normalized.get(index);
            if (request.id() == null) {
                SalesOrderShipment shipment = new SalesOrderShipment();
                shipment.setTenantCode(tenantCode);
                shipment.setOrderId(orderId);
                shipment.setLogisticsCompany(request.logisticsCompany());
                shipment.setTrackingNo(request.trackingNo());
                shipment.setSortOrder(index);
                shipment.setVersion(0);
                shipment.setCreator(user);
                shipment.setUpdater(user);
                shipment.setUpdaterName(userName);
                shipment.setCreateTime(now);
                shipment.setUpdateTime(now);
                int inserted = shipmentMapper.insert(shipment);
                if (inserted != 1) {
                    throw new BusinessException(500, "Failed to add shipment");
                }
                pendingEvents.add(new ShipmentEvent(shipment.getId(), request.trackingNo(), true));
                continue;
            }

            SalesOrderShipment existing = existingById.get(request.id());
            if (request.version() == null || !request.version().equals(existing.getVersion())) {
                throw new BusinessException(409, "Shipment has been modified by another user");
            }
            if (!hasChanged(existing, request, index)) {
                continue;
            }
            int changed = shipmentMapper.updateShipment(existing.getId(), tenantCode, orderId, request.version(),
                    request.logisticsCompany(), request.trackingNo(), index, user, userName, now);
            if (changed != 1) {
                throw new BusinessException(409, "Shipment has been modified by another user");
            }
            pendingEvents.add(new ShipmentEvent(existing.getId(), request.trackingNo(), false));
        }
        List<SalesOrderShipmentVO> result = listShipments(tenantCode, orderId);
        publishShipmentEvents(tenantCode, orderId, userId, pendingEvents, now);
        return result;
    }

    public List<SalesOrderShipmentVO> listShipments(String tenantCode, String orderId) {
        return selectShipments(tenantCode, orderId).stream().map(this::toVO).toList();
    }

    public Map<String, List<SalesOrderShipmentVO>> listShipmentsByOrderIds(String tenantCode,
                                                                              Collection<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> distinctOrderIds = orderIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (distinctOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<SalesOrderShipmentVO>> result = new LinkedHashMap<>();
        for (String orderId : distinctOrderIds) {
            result.put(orderId, new ArrayList<>());
        }
        shipmentMapper.selectList(new LambdaQueryWrapper<SalesOrderShipment>()
                        .eq(SalesOrderShipment::getTenantCode, tenantCode)
                        .in(SalesOrderShipment::getOrderId, distinctOrderIds)
                        .orderByAsc(SalesOrderShipment::getOrderId)
                        .orderByAsc(SalesOrderShipment::getSortOrder)
                        .orderByAsc(SalesOrderShipment::getId))
                .forEach(shipment -> result.get(shipment.getOrderId()).add(toVO(shipment)));
        return result;
    }

    public SalesOrderShipment requireShipment(String tenantCode, String orderId, Long shipmentId) {
        if (shipmentId == null) {
            throw new BusinessException("Shipment does not exist or does not belong to this order");
        }
        SalesOrderShipment shipment = shipmentMapper.selectOne(new LambdaQueryWrapper<SalesOrderShipment>()
                .eq(SalesOrderShipment::getTenantCode, tenantCode)
                .eq(SalesOrderShipment::getOrderId, orderId)
                .eq(SalesOrderShipment::getId, shipmentId)
                .last("LIMIT 1"));
        if (shipment == null) {
            throw new BusinessException("Shipment does not exist or does not belong to this order");
        }
        return shipment;
    }

    private List<NormalizedShipment> normalizeRequests(List<SalesOrderShipmentSaveRequest> requests) {
        List<SalesOrderShipmentSaveRequest> safeRequests = requests == null ? List.of() : requests;
        if (safeRequests.size() > MAX_SHIPMENTS) {
            throw new BusinessException("At most 50 shipments are allowed");
        }
        Set<String> trackingNumbers = new LinkedHashSet<>();
        List<NormalizedShipment> normalized = new ArrayList<>(safeRequests.size());
        for (SalesOrderShipmentSaveRequest request : safeRequests) {
            String company = trimRequired(request == null ? null : request.getLogisticsCompany());
            String trackingNo = trimRequired(request == null ? null : request.getTrackingNo());
            if (!trackingNumbers.add(trackingNo)) {
                throw new BusinessException("Duplicate tracking number");
            }
            normalized.add(new NormalizedShipment(request.getId(), request.getVersion(), company, trackingNo));
        }
        return normalized;
    }

    private String trimRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("Logistics company and tracking number are required");
        }
        return value.trim();
    }

    private List<SalesOrderShipment> selectShipments(String tenantCode, String orderId) {
        return shipmentMapper.selectList(new LambdaQueryWrapper<SalesOrderShipment>()
                .eq(SalesOrderShipment::getTenantCode, tenantCode)
                .eq(SalesOrderShipment::getOrderId, orderId)
                .orderByAsc(SalesOrderShipment::getSortOrder)
                .orderByAsc(SalesOrderShipment::getId));
    }

    private Long requireCurrentUserId() {
        Long userId = TenantPermissionContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "Login session has expired");
        }
        return userId;
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

    private boolean hasChanged(SalesOrderShipment existing, NormalizedShipment request, int sortOrder) {
        return !request.logisticsCompany().equals(existing.getLogisticsCompany())
                || !request.trackingNo().equals(existing.getTrackingNo())
                || !Integer.valueOf(sortOrder).equals(existing.getSortOrder());
    }

    private void publishShipmentEvents(String tenantCode,
                                       String orderId,
                                       Long userId,
                                       List<ShipmentEvent> pendingEvents,
                                       LocalDateTime createTime) {
        if (pendingEvents.isEmpty()) {
            return;
        }
        List<OperationLogEvent> events = pendingEvents.stream()
                .map(pending -> buildShipmentEvent(tenantCode, orderId, userId, pending, createTime))
                .toList();
        Runnable publish = () -> events.forEach(operationLogCollector::collect);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private OperationLogEvent buildShipmentEvent(String tenantCode,
                                                  String orderId,
                                                  Long userId,
                                                  ShipmentEvent pending,
                                                  LocalDateTime createTime) {
        String trackingNo = pending.trackingNo();
        boolean isNew = pending.isNew();
        String fingerprint = externalApiGuardService.fingerprint(trackingNo);
        OperationLogEvent event = new OperationLogEvent();
        event.setTenantCode(tenantCode);
        event.setUserId(userId);
        event.setModule("order");
        event.setAction(isNew ? "add_order_shipment" : "update_order_shipment");
        event.setBizType("order_shipment");
        event.setBizNo(orderId);
        event.setDescription(isNew
                ? "\u65b0\u589e\u8ba2\u5355\u7269\u6d41\u8bb0\u5f55"
                : "\u66f4\u65b0\u8ba2\u5355\u7269\u6d41\u8bb0\u5f55");
        event.setArgsJson("{\"shipmentId\":" + pending.shipmentId()
                + ",\"trackingFingerprint\":\"" + fingerprint + "\"}");
        event.setSuccess(true);
        event.setCreateTime(createTime);
        return event;
    }

    private SalesOrderShipmentVO toVO(SalesOrderShipment shipment) {
        SalesOrderShipmentVO vo = new SalesOrderShipmentVO();
        BeanUtils.copyProperties(shipment, vo);
        return vo;
    }

    private record NormalizedShipment(Long id, Integer version, String logisticsCompany, String trackingNo) {
    }

    private record ShipmentEvent(Long shipmentId, String trackingNo, boolean isNew) {
    }
}
