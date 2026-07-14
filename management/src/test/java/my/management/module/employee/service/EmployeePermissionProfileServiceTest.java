package my.management.module.employee.service;

import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.management.common.utils.PermissionCacheUtil;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.mapper.EmployeeChangeLogMapper;
import my.management.module.employee.model.dto.EmployeePermissionOverrideRequest;
import my.management.module.employee.model.entity.Employee;
import my.management.module.employee.model.vo.EmployeePermissionNodeVO;
import my.management.module.employee.model.vo.EmployeePermissionProfileVO;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.mapper.SysUserPermissionMapper;
import my.management.module.sys.mapper.SysUserRoleMapper;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.entity.SysRole;
import my.management.module.sys.model.entity.SysRolePermission;
import my.management.module.sys.model.entity.SysUserPermission;
import my.hive.shared.permission.PermissionCatalogV3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeePermissionProfileServiceTest {

    private static final String TENANT_CODE = "tenant-001";
    private static final long USER_ID = 42L;

    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private EmployeeChangeLogMapper employeeChangeLogMapper;
    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysRolePermissionMapper rolePermissionMapper;
    @Mock
    private SysUserPermissionMapper userPermissionMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private PermissionCacheUtil permissionCacheUtil;

    private PermissionCatalogV3 catalog;
    private EmployeePermissionProfileService service;
    private List<SysPermission> permissions;
    private Map<String, SysPermission> permissionByCode;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init(TENANT_CODE, 7L, Set.of("employee:permission:manage"));
        catalog = new PermissionCatalogV3();
        service = new EmployeePermissionProfileService();
        ReflectionTestUtils.setField(service, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(service, "employeeChangeLogMapper", employeeChangeLogMapper);
        ReflectionTestUtils.setField(service, "sysPermissionMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "sysRoleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "sysRolePermissionMapper", rolePermissionMapper);
        ReflectionTestUtils.setField(service, "sysUserPermissionMapper", userPermissionMapper);
        ReflectionTestUtils.setField(service, "sysUserRoleMapper", userRoleMapper);
        ReflectionTestUtils.setField(service, "permissionCacheUtil", permissionCacheUtil);
        ReflectionTestUtils.setField(service, "permissionCatalog", catalog);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        permissions = catalogPermissions();
        permissionByCode = new LinkedHashMap<>();
        permissions.forEach(permission -> permissionByCode.put(permission.getPermCode(), permission));
        when(employeeMapper.selectOne(any())).thenReturn(employee(5L));
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void buildsOneEffectiveTreeWithRoleSourcesAndPersonalOverrides() {
        when(employeeMapper.selectOne(any())).thenReturn(employee(5L, 2));
        when(permissionMapper.selectList(any())).thenReturn(permissions);
        SysRole salesRole = new SysRole();
        salesRole.setId(10L);
        salesRole.setRoleCode("sales");
        salesRole.setRoleName("Sales");
        salesRole.setTenantCode(TENANT_CODE);
        salesRole.setIsDeleted(0);
        when(userRoleMapper.selectRoleIdsByUserIdAndTenantCode(USER_ID, TENANT_CODE)).thenReturn(List.of(10L));
        when(roleMapper.selectList(any())).thenReturn(List.of(salesRole));
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(
                rolePermission(10L, permissionByCode.get("order:list").getId())));
        when(userPermissionMapper.selectList(any())).thenReturn(List.of(
                userPermission("order:list", SysUserPermission.EFFECT_DENY),
                userPermission("order:detail", SysUserPermission.EFFECT_GRANT)));

        EmployeePermissionProfileVO profile = service.profile(USER_ID);

        assertEquals(USER_ID, profile.getUserId());
        assertEquals(5L, profile.getPermissionVersion());
        assertEquals(List.of("sales"), profile.getRoles().stream().map(role -> role.getRoleCode()).toList());

        EmployeePermissionNodeVO deniedRolePermission = find(profile.getPermissions(), "order:list");
        assertTrue(deniedRolePermission.getRoleGranted());
        assertEquals(List.of("sales"), deniedRolePermission.getRoleSources().stream()
                .map(source -> source.getRoleCode()).toList());
        assertEquals(SysUserPermission.EFFECT_DENY, deniedRolePermission.getPersonalEffect());
        assertFalse(deniedRolePermission.getEffective());
        assertEquals("PERSONAL_DENY", deniedRolePermission.getEffectiveSource());

        EmployeePermissionNodeVO personalGrant = find(profile.getPermissions(), "order:detail");
        assertFalse(personalGrant.getRoleGranted());
        assertEquals(SysUserPermission.EFFECT_GRANT, personalGrant.getPersonalEffect());
        assertTrue(personalGrant.getEffective());
        assertEquals("PERSONAL_GRANT", personalGrant.getEffectiveSource());
    }

    @Test
    void attachesAndSortsProfileNodesIndependentlyOfCatalogQueryOrder() {
        List<SysPermission> reversedPermissions = new ArrayList<>(permissions);
        Collections.reverse(reversedPermissions);
        when(permissionMapper.selectList(any())).thenReturn(reversedPermissions);

        EmployeePermissionProfileVO profile = service.profile(USER_ID);

        assertEquals(childCodes(permissions, 0L), profile.getPermissions().stream()
                .map(EmployeePermissionNodeVO::getCode)
                .toList());
        for (SysPermission permission : permissions) {
            EmployeePermissionNodeVO node = find(profile.getPermissions(), permission.getPermCode());
            assertEquals(childCodes(permissions, permission.getId()), node.getChildren().stream()
                    .map(EmployeePermissionNodeVO::getCode)
                    .toList());
        }
    }

    @Test
    void rejectsStaleVersionBeforeReplacingOverrides() {
        EmployeePermissionOverrideRequest request = new EmployeePermissionOverrideRequest();
        request.setPermissionVersion(4L);
        request.setGrants(Set.of("order:detail"));
        request.setDenies(Set.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOverrides(USER_ID, request));

        assertEquals(409, exception.getCode());
        verify(employeeMapper, never()).incrementPermissionVersionIfCurrent(any(), any(), any());
        verify(userPermissionMapper, never()).delete(any());
        verify(userPermissionMapper, never()).upsertBatch(any());
    }

    @Test
    void rejectsPermissionChangesForResignedEmployee() {
        when(employeeMapper.selectOne(any())).thenReturn(employee(5L, 0));
        EmployeePermissionOverrideRequest request = new EmployeePermissionOverrideRequest();
        request.setPermissionVersion(5L);
        request.setGrants(Set.of("order:detail"));
        request.setDenies(Set.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOverrides(USER_ID, request));

        assertEquals(403, exception.getCode());
        verify(employeeMapper, never()).incrementPermissionVersionIfCurrent(any(), any(), any());
    }

    @Test
    void rejectsPermissionProfilesForResignedEmployee() {
        when(employeeMapper.selectOne(any())).thenReturn(employee(5L, 0));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.profile(USER_ID));

        assertEquals(403, exception.getCode());
        verify(permissionMapper, never()).selectList(any());
    }

    @Test
    void savesExactCodesAndAdvancesPermissionVersionWithoutChangingRoles() {
        when(permissionMapper.selectList(any())).thenReturn(permissions);
        EmployeePermissionOverrideRequest request = new EmployeePermissionOverrideRequest();
        request.setPermissionVersion(5L);
        request.setGrants(Set.of("order:detail"));
        request.setDenies(Set.of("order:list"));
        when(employeeMapper.incrementPermissionVersionIfCurrent(TENANT_CODE, USER_ID, 5L)).thenReturn(1);

        long nextVersion = service.updateOverrides(USER_ID, request);

        assertEquals(6L, nextVersion);
        verify(employeeMapper).incrementPermissionVersionIfCurrent(TENANT_CODE, USER_ID, 5L);
        verify(userPermissionMapper).delete(any());
        verify(userPermissionMapper).upsertBatch(any());
        verify(permissionCacheUtil).evict(TENANT_CODE, USER_ID);
        verify(employeeChangeLogMapper).insert(any());
        verify(userRoleMapper, never()).delete(any());
        verify(rolePermissionMapper, never()).delete(any());
    }

    private Employee employee(long permissionVersion) {
        return employee(permissionVersion, 1);
    }

    private Employee employee(long permissionVersion, int status) {
        Employee employee = new Employee();
        employee.setId(USER_ID);
        employee.setTenantCode(TENANT_CODE);
        employee.setPermissionVersion(permissionVersion);
        employee.setStatus(status);
        return employee;
    }

    private SysRolePermission rolePermission(long roleId, long permissionId) {
        SysRolePermission relation = new SysRolePermission();
        relation.setRoleId(roleId);
        relation.setPermissionId(permissionId);
        relation.setIsDeleted(0);
        return relation;
    }

    private SysUserPermission userPermission(String code, String effect) {
        SysUserPermission relation = new SysUserPermission();
        relation.setTenantCode(TENANT_CODE);
        relation.setUserId(USER_ID);
        relation.setPermissionId(permissionByCode.get(code).getId());
        relation.setEffect(effect);
        relation.setIsDeleted(0);
        return relation;
    }

    private List<SysPermission> catalogPermissions() {
        Map<String, Long> ids = new LinkedHashMap<>();
        long id = 1L;
        for (PermissionCatalogV3.PermissionDefinition definition : catalog.definitions()) {
            ids.put(definition.code(), id++);
        }
        List<SysPermission> rows = new ArrayList<>();
        for (PermissionCatalogV3.PermissionDefinition definition : catalog.definitions()) {
            SysPermission permission = new SysPermission();
            permission.setId(ids.get(definition.code()));
            permission.setParentId(definition.parentCode() == null ? 0L : ids.get(definition.parentCode()));
            permission.setPermCode(definition.code());
            permission.setPermName(definition.name());
            permission.setModuleCode(definition.moduleCode());
            permission.setPermType(definition.type().databaseCode());
            permission.setAssignable(definition.assignable() ? 1 : 0);
            permission.setStatus(1);
            permission.setSort(definition.sort());
            permission.setIsDeleted(0);
            rows.add(permission);
        }
        return rows;
    }

    private List<String> childCodes(List<SysPermission> permissions, Long parentId) {
        return permissions.stream()
                .filter(permission -> Objects.equals(permission.getParentId(), parentId))
                .sorted(Comparator.comparing(SysPermission::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysPermission::getId, Comparator.nullsLast(Long::compareTo)))
                .map(SysPermission::getPermCode)
                .toList();
    }

    private EmployeePermissionNodeVO find(List<EmployeePermissionNodeVO> nodes, String code) {
        for (EmployeePermissionNodeVO node : nodes) {
            if (code.equals(node.getCode())) {
                return node;
            }
            EmployeePermissionNodeVO child = findOrNull(node.getChildren(), code);
            if (child != null) {
                return child;
            }
        }
        throw new AssertionError("Permission node not found: " + code);
    }

    private EmployeePermissionNodeVO findOrNull(List<EmployeePermissionNodeVO> nodes, String code) {
        for (EmployeePermissionNodeVO node : nodes) {
            if (code.equals(node.getCode())) {
                return node;
            }
            EmployeePermissionNodeVO child = findOrNull(node.getChildren(), code);
            if (child != null) {
                return child;
            }
        }
        return null;
    }
}
