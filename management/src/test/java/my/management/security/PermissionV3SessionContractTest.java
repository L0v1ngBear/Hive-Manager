package my.management.security;

import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.hive.shared.redis.HiveRedisKeyProperties;
import my.management.common.utils.PermissionCacheUtil;
import my.management.module.auth.model.vo.LoginUserRow;
import my.management.module.employee.mapper.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionV3SessionContractTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java");

    @Test
    void loginRowsAndEmployeeMapperExposeSessionVersions() {
        assertDoesNotThrow(() -> LoginUserRow.class.getDeclaredField("permissionVersion"));
        assertDoesNotThrow(() -> LoginUserRow.class.getDeclaredField("authVersion"));
        assertDoesNotThrow(() -> EmployeeMapper.class.getDeclaredMethod(
                "incrementPermissionVersion", String.class, Long.class));
        assertDoesNotThrow(() -> EmployeeMapper.class.getDeclaredMethod(
                "incrementAuthVersion", String.class, Long.class));
    }

    @Test
    void permissionCacheKeyChangesWithDatabaseVersion() throws Exception {
        PermissionCacheUtil cache = new PermissionCacheUtil();
        ReflectionTestUtils.setField(cache, "redisKeyBuilder",
                new HiveRedisKeyBuilder(new HiveRedisKeyProperties()));
        Method method = PermissionCacheUtil.class.getDeclaredMethod(
                "buildManagementKey", String.class, Long.class, Long.class);
        method.setAccessible(true);

        String before = (String) method.invoke(cache, "TENANT_001", 7L, 3L);
        String after = (String) method.invoke(cache, "TENANT_001", 7L, 4L);

        assertNotEquals(before, after);
        assertTrue(before.contains("perm-v3"));
        assertTrue(before.endsWith(":3:3"));
    }

    @Test
    void interceptorValidatesAccountBeforePermissionLookupAndRenewsCurrentVersion() throws IOException {
        String source = readJava("my/management/common/interceptor/TenantContextFilter.java");
        int authCheck = source.indexOf("getAuthVersion()");
        int permissionLoad = source.indexOf("effectivePermissionService.resolve");

        assertTrue(authCheck >= 0);
        assertTrue(permissionLoad > authCheck);
        assertTrue(source.contains("isUsableEmployeeStatus"));
        assertTrue(source.contains("currentAuthVersion"));
    }

    @Test
    void loginUsesUnifiedEffectivePermissionResolutionOnly() throws IOException {
        String source = readJava("my/management/module/auth/service/AuthService.java");
        assertTrue(source.contains("effectivePermissionService.resolve(loginUser.getUserId(), loginUser.getTenantCode())"));
        assertFalse(source.contains("authMapper.selectPermCodesByUserIdAndTenantCode(loginUser.getUserId()"));
    }

    @Test
    void permissionSqlAndApproverQueriesUseEnabledExactLeavesOnly() throws IOException {
        String authMapper = readJava("my/management/module/auth/mapper/AuthMapper.java");
        String employeeMapper = readJava("my/management/module/employee/mapper/EmployeeMapper.java");

        assertTrue(authMapper.contains("p.status = 1"));
        assertTrue(authMapper.contains("p.assignable = 1"));
        assertTrue(employeeMapper.contains("p.status = 1"));
        assertTrue(employeeMapper.contains("p.assignable = 1"));
        assertFalse(employeeMapper.contains("SUBSTRING_INDEX"));
        assertFalse(employeeMapper.contains("'*:*'"));
    }

    @Test
    void permissionMutationsRotateVersionsAndEvictSharedStateAfterCommit() throws IOException {
        String cache = readJava("my/management/common/utils/PermissionCacheUtil.java");
        String employee = readJava("my/management/module/employee/service/EmployeeService.java");
        String role = readJava("my/management/module/sys/service/RoleService.java");

        assertTrue(cache.contains("TransactionSynchronizationManager.registerSynchronization"));
        assertTrue(cache.contains("\"auth\", \"account-v3\""));
        assertFalse(cache.contains("perm-v2"));
        assertTrue(employee.contains("incrementPermissionVersion"));
        assertTrue(employee.contains("incrementAuthVersion"));
        assertTrue(role.contains("incrementPermissionVersion"));
    }

    @Test
    void attendanceEndpointsUseTheirExactPermissions() throws IOException {
        String source = readJava("my/management/controller/AttendanceManageController.java");

        assertTrue(source.contains("CODE_ATTENDANCE_RULE_LIST"));
        assertTrue(source.contains("CODE_ATTENDANCE_RULE_UPDATE"));
        assertTrue(source.contains("CODE_ATTENDANCE_EXPORT"));
        assertFalse(source.contains("CODE_ATTENDANCE_ALL"));
    }

    private String readJava(String relativePath) throws IOException {
        return Files.readString(MAIN_JAVA.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
