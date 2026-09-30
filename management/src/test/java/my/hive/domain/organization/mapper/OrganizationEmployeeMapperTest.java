package my.hive.domain.organization.mapper;

import my.hive.domain.organization.model.vo.OrganizationPositionVO;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationEmployeeMapperTest {

    @Test
    void departmentAndPositionCountsExcludeDeletedEmployeesButKeepLegacyEmployeesWithoutExtension() throws Exception {
        DataSource dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:organization_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=FALSE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
                "sa", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE user (id BIGINT PRIMARY KEY, tenant_code VARCHAR(64), name VARCHAR(64), "
                    + "department_name VARCHAR(64), position VARCHAR(64), status INT, phone VARCHAR(32), phone_mask VARCHAR(32))");
            statement.execute("CREATE TABLE emp_employee_ext (id BIGINT PRIMARY KEY, user_id BIGINT, tenant_code VARCHAR(64), "
                    + "is_deleted INT, emp_no VARCHAR(64))");
            statement.execute("CREATE TABLE emp_department (id BIGINT PRIMARY KEY, tenant_code VARCHAR(64), "
                    + "dept_name VARCHAR(64), is_deleted INT)");
            statement.execute("CREATE TABLE emp_position (id BIGINT PRIMARY KEY, tenant_code VARCHAR(64), department_id BIGINT, "
                    + "position_name VARCHAR(64), position_code VARCHAR(64), sort_no INT, status INT, is_deleted INT, "
                    + "create_time TIMESTAMP, update_time TIMESTAMP)");
            statement.execute("INSERT INTO emp_department VALUES (2, 'TENANT_001', '销售部', 0)");
            statement.execute("INSERT INTO emp_position VALUES (9, 'TENANT_001', 2, '销售专员', 'POS-9', 1, 1, 0, NULL, NULL)");
            statement.execute("INSERT INTO user VALUES "
                    + "(1, 'TENANT_001', '在职', '销售部', '销售专员', 1, NULL, NULL),"
                    + "(2, 'TENANT_001', '已删除', '销售部', '销售专员', 0, NULL, NULL),"
                    + "(3, 'TENANT_001', '历史员工', '销售部', '销售专员', 1, NULL, NULL),"
                    + "(4, 'TENANT_002', '其他租户', '销售部', '销售专员', 1, NULL, NULL),"
                    + "(5, 'TENANT_001', '总部员工', '总部', '总经理', 1, NULL, NULL)");
            statement.execute("INSERT INTO emp_employee_ext VALUES "
                    + "(11, 1, 'TENANT_001', 0, 'E-1'),"
                    + "(12, 2, 'TENANT_001', 1, 'E-2'),"
                    + "(14, 4, 'TENANT_002', 0, 'E-4')");
        }

        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(OrganizationMapper.class);
        try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession()) {
            OrganizationMapper mapper = session.getMapper(OrganizationMapper.class);

            assertThat(mapper.selectDepartmentEmployeeCounts("TENANT_001"))
                    .extracting(this::count)
                    .containsExactlyInAnyOrder(2L, 1L);
            assertThat(mapper.selectEmployeesByDepartment("TENANT_001", "销售部"))
                    .extracting(employee -> employee.getName())
                    .containsExactlyInAnyOrder("在职", "历史员工");
            assertThat(mapper.selectEmployeesByDepartments("TENANT_001", java.util.List.of("总部", "销售部")))
                    .extracting(employee -> employee.getName())
                    .containsExactlyInAnyOrder("总部员工", "在职", "历史员工");
            assertThat(mapper.countEmployeesByPosition("TENANT_001", "销售部", "销售专员"))
                    .isEqualTo(2L);
            assertThat(mapper.selectPositions("TENANT_001", 2L))
                    .extracting(OrganizationPositionVO::getEmployeeCount)
                    .containsExactly(2L);
        }
    }

    private long count(Map<String, Object> row) {
        return ((Number) row.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("employeeCount"))
                .findFirst()
                .orElseThrow()
                .getValue()).longValue();
    }
}
