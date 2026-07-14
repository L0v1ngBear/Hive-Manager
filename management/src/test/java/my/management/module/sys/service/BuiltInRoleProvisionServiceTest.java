package my.management.module.sys.service;

import my.hive.shared.permission.PermissionCatalogV3;

import my.hive.shared.exception.BusinessException;
import my.management.module.sys.mapper.SysPermissionMapper;
import my.management.module.sys.mapper.SysRoleMapper;
import my.management.module.sys.mapper.SysRolePermissionMapper;
import my.management.module.sys.model.entity.SysPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuiltInRoleProvisionServiceTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    private BuiltInRoleProvisionService service;

    @BeforeEach
    void setUp() {
        PermissionCatalogV3 permissionCatalog = new PermissionCatalogV3();
        service = new BuiltInRoleProvisionService();
        ReflectionTestUtils.setField(service, "permissionCatalog", permissionCatalog);
        ReflectionTestUtils.setField(service, "builtInRoleCatalog", new BuiltInRoleCatalog(permissionCatalog));
        ReflectionTestUtils.setField(service, "sysRoleMapper", sysRoleMapper);
        ReflectionTestUtils.setField(service, "sysPermissionMapper", sysPermissionMapper);
        ReflectionTestUtils.setField(service, "sysRolePermissionMapper", sysRolePermissionMapper);
    }

    @Test
    void missingV3LeavesAbortBeforeAnyRoleIsCreated() {
        SysPermission legacyWildcard = new SysPermission();
        legacyWildcard.setId(99L);
        legacyWildcard.setPermCode("inventory:*");
        legacyWildcard.setAssignable(1);
        legacyWildcard.setStatus(1);

        when(sysRoleMapper.selectList(any())).thenReturn(List.of());
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(legacyWildcard));

        assertThrows(BusinessException.class, () -> service.ensureTenantRoles("TENANT-NEW"));
        verify(sysRoleMapper, never()).insert(any());
        verify(sysRolePermissionMapper, never()).upsertBatch(any());
    }
}
