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
import java.util.UUID;

@Service
public class OrderShipmentService {

    private static final int MAX_SHIPMENTS = 50;
    private static final String DELIVERY_MODE_TRACKED = "tracked";
    private static final Set<String> DELIVERY_MODES = Set.of(
            DELIVERY_MODE_TRACKED, "lalamove", "self_delivery", "customer_pickup", "other");

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
                throw new BusinessException("发货记录不存在或不属于当前订单");
            }
            if (!submittedExistingIds.add(shipment.id())) {
                throw new BusinessException("发货记录编号重复");
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
                shipment.setDeliveryMode(request.deliveryMode());
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
                    throw new BusinessException(500, "新增发货记录失败");
                }
                pendingEvents.add(new ShipmentEvent(shipment.getId(), request.trackingNo(), true));
                continue;
            }

            SalesOrderShipment existing = existingById.get(request.id());
            if (request.version() == null || !request.version().equals(existing.getVersion())) {
                throw new BusinessException(409, "发货记录已被其他人修改，请刷新后重试");
            }
            if (!hasChanged(existing, request, index)) {
                continue;
            }
            int changed = shipmentMapper.updateShipment(existing.getId(), tenantCode, orderId, request.version(),
                    request.deliveryMode(), request.logisticsCompany(), request.trackingNo(), index, user, userName, now);
            if (changed != 1) {
                throw new BusinessException(409, "发货记录已被其他人修改，请刷新后重试");
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
            throw new BusinessException("发货记录不存在或不属于当前订单");
        }
        SalesOrderShipment shipment = shipmentMapper.selectOne(new LambdaQueryWrapper<SalesOrderShipment>()
                .eq(SalesOrderShipment::getTenantCode, tenantCode)
                .eq(SalesOrderShipment::getOrderId, orderId)
                .eq(SalesOrderShipment::getId, shipmentId)
                .last("LIMIT 1"));
        if (shipment == null) {
            throw new BusinessException("发货记录不存在或不属于当前订单");
        }
        return shipment;
    }

    private List<NormalizedShipment> normalizeRequests(List<SalesOrderShipmentSaveRequest> requests) {
        List<SalesOrderShipmentSaveRequest> safeRequests = requests == null ? List.of() : requests;
        if (safeRequests.size() > MAX_SHIPMENTS) {
            throw new BusinessException("每个订单最多允许 50 条发货记录");
        }
        Set<String> trackingNumbers = new LinkedHashSet<>();
        List<NormalizedShipment> normalized = new ArrayList<>(safeRequests.size());
        for (SalesOrderShipmentSaveRequest request : safeRequests) {
            String deliveryMode = normalizeDeliveryMode(request == null ? null : request.getDeliveryMode(),
                    request == null ? null : request.getTrackingNo());
            String company = trimOptional(request == null ? null : request.getLogisticsCompany());
            String trackingNo = trimOptional(request == null ? null : request.getTrackingNo());
            if (DELIVERY_MODE_TRACKED.equals(deliveryMode)) {
                company = trimRequired(company, "快递物流必须填写物流公司");
                trackingNo = trimRequired(trackingNo, "快递物流必须填写物流单号");
                if (!trackingNumbers.add(trackingNo)) {
                    throw new BusinessException("物流单号不能重复");
                }
            } else if (trackingNo != null) {
                throw new BusinessException("非可追踪发货方式不能填写物流单号");
            }
            normalized.add(new NormalizedShipment(request.getId(), request.getVersion(), deliveryMode, company, trackingNo));
        }
        return normalized;
    }

    private String normalizeDeliveryMode(String deliveryMode, String trackingNo) {
        String normalized = trimOptional(deliveryMode);
        if (normalized == null) {
            return StringUtils.hasText(trackingNo) ? DELIVERY_MODE_TRACKED : "other";
        }
        if (!DELIVERY_MODES.contains(normalized)) {
            throw new BusinessException("发货方式无效");
        }
        return normalized;
    }

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String trimOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
            throw new BusinessException(401, "登录状态已失效，请重新登录");
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
        return !request.deliveryMode().equals(normalizeExistingDeliveryMode(existing))
                || !java.util.Objects.equals(request.logisticsCompany(), existing.getLogisticsCompany())
                || !java.util.Objects.equals(request.trackingNo(), existing.getTrackingNo())
                || !Integer.valueOf(sortOrder).equals(existing.getSortOrder());
    }

    private String normalizeExistingDeliveryMode(SalesOrderShipment shipment) {
        return normalizeDeliveryMode(shipment.getDeliveryMode(), shipment.getTrackingNo());
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
        String fingerprint = StringUtils.hasText(trackingNo)
                ? externalApiGuardService.fingerprint(trackingNo)
                : null;
        OperationLogEvent event = new OperationLogEvent();
        event.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        event.setTenantCode(tenantCode);
        event.setUserId(userId);
        event.setModule("order");
        event.setAction(isNew ? "add_order_shipment" : "update_order_shipment");
        event.setBizType("order_shipment");
        event.setBizNo(orderId);
        event.setDescription(isNew
                ? "\u65b0\u589e\u8ba2\u5355\u7269\u6d41\u8bb0\u5f55"
                : "\u66f4\u65b0\u8ba2\u5355\u7269\u6d41\u8bb0\u5f55");
        event.setArgsJson(fingerprint == null
                ? "{\"shipmentId\":" + pending.shipmentId() + "}"
                : "{\"shipmentId\":" + pending.shipmentId()
                        + ",\"trackingFingerprint\":\"" + fingerprint + "\"}");
        event.setLogLevel("INFO");
        event.setSuccess(true);
        event.setSlow(false);
        event.setDurationMs(0L);
        event.setCreateTime(createTime);
        return event;
    }

    private SalesOrderShipmentVO toVO(SalesOrderShipment shipment) {
        SalesOrderShipmentVO vo = new SalesOrderShipmentVO();
        BeanUtils.copyProperties(shipment, vo);
        return vo;
    }

    private record NormalizedShipment(Long id, Integer version, String deliveryMode, String logisticsCompany, String trackingNo) {
    }

    private record ShipmentEvent(Long shipmentId, String trackingNo, boolean isNew) {
    }
}
