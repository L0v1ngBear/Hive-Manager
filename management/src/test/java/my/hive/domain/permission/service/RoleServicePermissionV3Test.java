package my.hive.domain.permission.service;

import my.hive.shared.permission.PermissionCatalogV3;

import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.permission.mapper.SysPermissionMapper;
import my.hive.domain.permission.mapper.SysRoleMapper;
import my.hive.domain.permission.mapper.SysRolePermissionMapper;
import my.hive.domain.permission.mapper.SysUserRoleMapper;
import my.hive.domain.permission.model.dto.SysRoleUpdateRequest;
import my.hive.domain.permission.model.entity.SysPermission;
import my.hive.domain.permission.model.entity.SysRole;
import my.hive.domain.permission.model.vo.SysPermissionTreeVO;
import my.hive.shared.utils.PermissionCacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServicePermissionV3Test {

    @Mock
    private SysPermissionMapper permissionMapper;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService();
        ReflectionTestUtils.setField(roleService, "sysPermissionMapper", permissionMapper);
        ReflectionTestUtils.setField(roleService, "permissionCatalog", new PermissionCatalogV3());
    }

    @Test
    void permissionTreeContainsOnlyEnabledV3Nodes() {
        SysPermission orderGroup = permission(1L, null, "order", 1, 0, 1);
        SysPermission orderList = permission(2L, 1L, "order:list", 2, 1, 1);
        SysPermission legacyWildcard = permission(3L, 1L, "order:*", 3, 1, 1);
        SysPermission disabledLeaf = permission(4L, 1L, "order:detail", 2, 1, 0);
        SysPermission platformLeaf = permission(5L, null, "platform:tenant:view", 2, 1, 1);
        when(permissionMapper.selectList(any())).thenReturn(List.of(
                orderGroup, orderList, legacyWildcard, disabledLeaf, platformLeaf));

        List<SysPermissionTreeVO> tree = roleService.selectAllPermissionTree();

        assertEquals(1, tree.size());
        assertEquals("order", tree.getFirst().getPermCode());
        assertEquals(0, tree.getFirst().getAssignable());
        assertEquals(List.of("order:list"), tree.getFirst().getChildren().stream()
                .map(SysPermissionTreeVO::getPermCode)
                .toList());
        Set<String> codes = flatten(tree);
        assertFalse(codes.contains("order:*"));
        assertFalse(codes.contains("order:detail"));
        assertFalse(codes.contains("platform:tenant:view"));
    }

    @Test
    void keepsSavingPermissionsWhenRoleHasAStaleEmployeeBinding() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
        EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
        PermissionCacheUtil permissionCacheUtil = mock(PermissionCacheUtil.class);
        ReflectionTestUtils.setField(roleService, "sysRoleMapper", roleMapper);
        ReflectionTestUtils.setField(roleService, "sysUserRoleMapper", userRoleMapper);
        ReflectionTestUtils.setField(roleService, "sysRolePermissionMapper", rolePermissionMapper);
        ReflectionTestUtils.setField(roleService, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(roleService, "permissionCacheUtil", permissionCacheUtil);

        SysRole role = new SysRole();
        role.setId(9L);
        role.setTenantCode("TENANT_A");
        role.setRoleCode("SALES_MANAGER");
        role.setIsDeleted(0);
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(permissionMapper.selectList(any()))
                .thenReturn(List.of(permission(70L, null, "document:file:download", 3, 1, 1)));
        // 角色上残留一条指向已不存在员工的脏绑定（7L），正常绑定为 8L：
        // 脏绑定只能被跳过并告警，不能回滚整次“分配权限”保存。
        when(userRoleMapper.selectUserIdsByRoleId("TENANT_A", 9L)).thenReturn(List.of(7L, 8L));
        when(employeeMapper.incrementPermissionVersion("TENANT_A", 7L)).thenReturn(0);
        when(employeeMapper.incrementPermissionVersion("TENANT_A", 8L)).thenReturn(1);
        my.hive.shared.context.TenantPermissionContext.init("TENANT_A", 1L, Set.of("role:update"));
        SysRoleUpdateRequest request = new SysRoleUpdateRequest();
        request.setRoleId(9L);
        request.setPermissionIds(List.of(70L));

        assertDoesNotThrow(() -> roleService.updateRole(request));

        verify(rolePermissionMapper).delete(any());
        verify(rolePermissionMapper).upsertBatch(any());
        verify(permissionCacheUtil).evict("TENANT_A", 8L);
        verify(permissionCacheUtil, never()).evict("TENANT_A", 7L);
        my.hive.shared.context.TenantPermissionContext.clear();
    }

    private Set<String> flatten(List<SysPermissionTreeVO> nodes) {
        return nodes.stream()
                .flatMap(node -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(node.getPermCode()),
                        flatten(node.getChildren()).stream()))
                .collect(Collectors.toSet());
    }

    private SysPermission permission(Long id,
                                     Long parentId,
                                     String code,
                                     int type,
                                     int assignable,
                                     int status) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setParentId(parentId);
        permission.setPermCode(code);
        permission.setPermName(code);
        permission.setModuleCode(code.contains(":") ? code.substring(0, code.indexOf(':')) : code);
        permission.setPermType(type);
        permission.setAssignable(assignable);
        permission.setStatus(status);
        permission.setSort(id.intValue());
        permission.setIsDeleted(0);
        return permission;
    }
}
