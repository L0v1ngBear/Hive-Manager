package my.hive.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.order.mapper.SalesOrderShipmentMapper;
import my.hive.domain.order.model.dto.SalesOrderShipmentSaveRequest;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.external.ExternalApiGuardService;
import my.hive.shared.log.OperationLogProperties;
import my.hive.shared.log.Slf4jOperationLogCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderShipmentOperationLogPersistenceTest {

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void shipmentOnlySavePersistsQueryableOrderAudit() {
        JdbcTemplate jdbcTemplate = operationLogDatabase();
        OperationLogProperties properties = new OperationLogProperties();
        properties.setCollectToDb(true);
        properties.setAsyncDb(false);
        properties.setCollectToSlf4j(false);
        Slf4jOperationLogCollector collector = new Slf4jOperationLogCollector(
                new ObjectMapper(), jdbcTemplate, mock(RabbitTemplate.class), properties);

        SalesOrderShipmentMapper shipmentMapper = mock(SalesOrderShipmentMapper.class);
        EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
        ExternalApiGuardService externalApiGuardService = mock(ExternalApiGuardService.class);
        OrderShipmentService service = new OrderShipmentService(
                shipmentMapper, employeeMapper, collector, externalApiGuardService);
        TenantPermissionContext.init("TENANT_001", 9L, Set.of());

        Employee employee = new Employee();
        employee.setId(9L);
        employee.setName("Test Operator");
        when(employeeMapper.selectOne(any())).thenReturn(employee);
        when(shipmentMapper.selectList(any())).thenReturn(List.of(), List.of(persistedShipment()));
        when(shipmentMapper.insert(any())).thenAnswer(invocation -> {
            invocation.<SalesOrderShipment>getArgument(0).setId(101L);
            return 1;
        });
        when(externalApiGuardService.fingerprint("SF-001")).thenReturn("tracking-fingerprint");

        SalesOrderShipmentSaveRequest request = new SalesOrderShipmentSaveRequest();
        request.setLogisticsCompany("SF Express");
        request.setTrackingNo("SF-001");
        service.saveShipments("TENANT_001", "SO-1", List.of(request));

        Map<String, Object> audit = jdbcTemplate.queryForMap("""
                SELECT trace_id, log_level, action, biz_type, biz_no, success, slow, duration_ms
                FROM operation_log
                WHERE tenant_code = ? AND module = 'order' AND biz_no = ? AND biz_type = 'order_shipment'
                """, "TENANT_001", "SO-1");
        assertFalse(String.valueOf(audit.get("TRACE_ID")).isBlank());
        assertEquals("INFO", audit.get("LOG_LEVEL"));
        assertEquals("add_order_shipment", audit.get("ACTION"));
        assertEquals("order_shipment", audit.get("BIZ_TYPE"));
        assertEquals("SO-1", audit.get("BIZ_NO"));
        assertEquals(1, ((Number) audit.get("SUCCESS")).intValue());
        assertEquals(0, ((Number) audit.get("SLOW")).intValue());
        assertEquals(0L, ((Number) audit.get("DURATION_MS")).longValue());
    }

    private JdbcTemplate operationLogDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shipment_operation_log;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS operation_log");
        jdbcTemplate.execute("""
                CREATE TABLE operation_log (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  trace_id VARCHAR(64) NOT NULL,
                  tenant_code VARCHAR(64),
                  user_id BIGINT,
                  module VARCHAR(64) NOT NULL,
                  action VARCHAR(64) NOT NULL,
                  biz_type VARCHAR(64),
                  biz_no VARCHAR(128),
                  description VARCHAR(255),
                  log_level VARCHAR(16) NOT NULL DEFAULT 'INFO',
                  class_name VARCHAR(255),
                  method_name VARCHAR(128),
                  request_method VARCHAR(16),
                  request_uri VARCHAR(500),
                  client_ip VARCHAR(64),
                  user_agent VARCHAR(500),
                  args_json CLOB,
                  result_json CLOB,
                  success TINYINT NOT NULL DEFAULT 1,
                  slow TINYINT NOT NULL DEFAULT 0,
                  duration_ms BIGINT NOT NULL DEFAULT 0,
                  error_type VARCHAR(255),
                  error_message CLOB,
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        return jdbcTemplate;
    }

    private SalesOrderShipment persistedShipment() {
        SalesOrderShipment shipment = new SalesOrderShipment();
        shipment.setId(101L);
        shipment.setTenantCode("TENANT_001");
        shipment.setOrderId("SO-1");
        shipment.setLogisticsCompany("SF Express");
        shipment.setTrackingNo("SF-001");
        shipment.setSortOrder(0);
        shipment.setVersion(0);
        return shipment;
    }
}
