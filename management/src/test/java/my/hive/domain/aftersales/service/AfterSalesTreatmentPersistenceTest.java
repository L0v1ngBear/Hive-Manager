package my.hive.domain.aftersales.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import my.hive.domain.aftersales.mapper.AfterSalesTreatmentMapper;
import my.hive.domain.aftersales.model.entity.AfterSalesTreatment;
import my.hive.shared.context.TenantPermissionContext;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.time.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class AfterSalesTreatmentPersistenceTest {
    @Test void migrationPreservesLegacyLedgerAndRealTodoSqlIsTenantAndAssigneeScoped() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:treatmentPersistence;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE after_sales_part_stock_record(id BIGINT PRIMARY KEY)");
            sql.execute("INSERT INTO after_sales_part_stock_record VALUES(1)");
            sql.execute("CREATE TABLE after_sales_ticket(id BIGINT PRIMARY KEY, tenant_code VARCHAR(64), assignee_user_id BIGINT)");
            try (var migration = Files.newBufferedReader(Path.of("../db-migrations/migrations/V20260919_002_after_sales_treatments.sql"))) {
                org.h2.tools.RunScript.execute(connection, migration);
            }
            sql.execute("INSERT INTO after_sales_ticket(id,tenant_code,assignee_user_id) VALUES(1,'T1',7),(2,'T1',8),(3,'T2',7)");
            try (var old = sql.executeQuery("SELECT treatment_id FROM after_sales_part_stock_record WHERE id=1")) { assertTrue(old.next()); assertNull(old.getObject(1)); }
        }
        var config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(), ds));
        var interceptor = new MybatisPlusInterceptor(); interceptor.addInnerInterceptor(new PaginationInnerInterceptor()); config.addInterceptor(interceptor);
        config.addMapper(AfterSalesTreatmentMapper.class);
        try (var session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(true)) {
            var mapper = session.getMapper(AfterSalesTreatmentMapper.class);
            var active = record(1, "T1", 1, "processing", 2);
            var old = record(2, "T1", 1, "unresolved", 1);
            mapper.insert(active); mapper.insert(old);
            mapper.insert(record(3, "T1", 2, "processing", 1));
            mapper.insert(record(4, "T2", 3, "processing", 1));
            TenantPermissionContext.init("T1", 7L, Set.of("after_sales:process"));
            var service = new AfterSalesTreatmentService(); ReflectionTestUtils.setField(service, "treatmentMapper", mapper);
            var page = service.myTasks(1, 10);
            assertEquals(1, page.getTotal()); assertEquals(1L, page.getData().get(0).getId());
            assertEquals(LocalDate.of(2020, 2, 29), page.getData().get(0).getOriginalOpeningDate());
            active.setStatus("resolved"); mapper.updateById(active);
            assertEquals(0, service.myTasks(1, 10).getTotal());
            old.setUpdateTime(LocalDateTime.of(2026, 9, 19, 12, 3)); mapper.updateById(old);
            assertEquals(2L, service.myTasks(1, 10).getData().get(0).getId(), "late unresolved follow-up needs a new treatment");
        } finally { TenantPermissionContext.clear(); }
    }
    private AfterSalesTreatment record(long id, String tenant, long ticket, String status, int minute) {
        var r = new AfterSalesTreatment(); r.setId(id); r.setTenantCode(tenant); r.setTicketId(ticket); r.setTicketNo("AS" + ticket);
        r.setSequenceNo((int) id); r.setRequestKey("request" + id); r.setRequestHash("hash"); r.setTreatmentType("motor_replacement");
        r.setStatus(status); r.setVersion(0); r.setOriginalOpeningDate(LocalDate.of(2020, 2, 29));
        r.setCreateTime(LocalDateTime.of(2026, 9, 19, 12, 0)); r.setUpdateTime(LocalDateTime.of(2026, 9, 19, 12, minute));
        r.setDetailsJson("{}"); r.setPartsJson("[]"); return r;
    }
}
