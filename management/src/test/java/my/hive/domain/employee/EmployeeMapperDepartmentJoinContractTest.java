package my.hive.domain.employee;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeMapperDepartmentJoinContractTest {

    @Test
    void employeeQueriesMatchSameNamedPositionsWithinTheEmployeesDepartment() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/my/hive/domain/employee/mapper/EmployeeMapper.java"),
                StandardCharsets.UTF_8);

        assertThat(source)
                .doesNotContain("p.position_name = u.position AND p.tenant_code = u.tenant_code")
                .contains("p.position_name = u.position AND p.department_id = d.id");
        assertThat(count(source, "p.position_name = u.position AND p.department_id = d.id"))
                .isEqualTo(3);
    }

    private int count(String source, String token) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(token, from)) >= 0) {
            count++;
            from += token.length();
        }
        return count;
    }
}
