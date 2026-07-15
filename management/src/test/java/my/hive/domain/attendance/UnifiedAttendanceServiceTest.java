package my.hive.domain.attendance;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedAttendanceServiceTest {

    private static final Path MAIN_SOURCE = Path.of("src/main/java");

    @Test
    void attendanceUsesOneCanonicalDomainAndController() {
        assertThat(MAIN_SOURCE.resolve("my/hive/domain/attendance/service/AttendanceService.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/api/attendance/AttendanceController.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/management/module/attendance")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/controller/AttendanceManageController.java")).doesNotExist();
    }

    @Test
    void canonicalControllerKeepsManagementAndMiniAttendanceOperations() throws Exception {
        String controller = Files.readString(MAIN_SOURCE.resolve("my/hive/api/attendance/AttendanceController.java"));

        assertThat(controller)
                .contains("/summary", "/page", "/departments", "/rule", "/rule/save", "/export-excel")
                .contains("/punch", "/records/me");
    }
}
