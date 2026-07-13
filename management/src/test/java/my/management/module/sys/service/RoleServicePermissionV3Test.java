package my.management.module.sys.service;

import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.model.entity.SysPermission;
import my.management.module.sys.model.vo.SysPermissionTreeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
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
