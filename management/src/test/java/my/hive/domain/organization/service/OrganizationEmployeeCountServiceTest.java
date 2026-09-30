package my.hive.domain.organization.service;

import my.hive.domain.employee.mapper.DepartmentMapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Department;
import my.hive.domain.organization.mapper.OrganizationMapper;
import my.hive.domain.organization.model.vo.OrganizationEmployeeVO;
import my.hive.domain.organization.model.vo.OrganizationOverviewVO;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationEmployeeCountServiceTest {

    private final DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
    private final EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
    private final OrganizationMapper organizationMapper = mock(OrganizationMapper.class);
    private final PrivacyProtectionUtil privacyProtectionUtil = mock(PrivacyProtectionUtil.class);
    private final OrganizationService service = new OrganizationService();

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of("organization:view"));
        ReflectionTestUtils.setField(service, "departmentMapper", departmentMapper);
        ReflectionTestUtils.setField(service, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(service, "organizationMapper", organizationMapper);
        ReflectionTestUtils.setField(service, "privacyProtectionUtil", privacyProtectionUtil);
    }

    @AfterEach
    void clearContext() {
        TenantPermissionContext.clear();
    }

    @Test
    void overviewIncludesDescendantEmployeesAtEveryAncestorWithoutChangingTenantTotal() {
        when(departmentMapper.selectList(any())).thenReturn(List.of(
                department(1L, null, "总部"),
                department(2L, 1L, "销售部"),
                department(3L, 2L, "华东组"),
                department(4L, null, "财务部")
        ));
        when(organizationMapper.selectDepartmentEmployeeCounts("TENANT_001")).thenReturn(List.of(
                Map.of("departmentName", "总部", "employeeCount", 1L),
                Map.of("departmentName", "销售部", "employeeCount", 2L),
                Map.of("departmentName", "华东组", "employeeCount", 3L),
                Map.of("departmentName", "财务部", "employeeCount", 1L)
        ));
        when(organizationMapper.selectDepartmentPositionCounts("TENANT_001")).thenReturn(List.of());
        when(employeeMapper.countAvailableEmployees("TENANT_001")).thenReturn(7L);

        OrganizationOverviewVO overview = service.overview();

        assertThat(overview.getDepartments()).hasSize(2);
        assertThat(overview.getDepartments().get(0).getEmployeeCount()).isEqualTo(6L);
        assertThat(overview.getDepartments().get(0).getChildren().get(0).getEmployeeCount()).isEqualTo(5L);
        assertThat(overview.getDepartments().get(0).getChildren().get(0).getChildren().get(0).getEmployeeCount())
                .isEqualTo(3L);
        assertThat(overview.getDepartments().get(1).getEmployeeCount()).isEqualTo(1L);
        assertThat(overview.getStats().getEmployeeCount()).isEqualTo(7L);
        assertThat(overview.getStats().getEmptyDepartmentCount()).isZero();
    }

    @Test
    void parentWithEmployeesOnlyInChildIsNotCountedAsEmpty() {
        when(departmentMapper.selectList(any())).thenReturn(List.of(
                department(1L, null, "总部"), department(2L, 1L, "销售部")
        ));
        when(organizationMapper.selectDepartmentEmployeeCounts("TENANT_001"))
                .thenReturn(List.of(Map.of("departmentName", "销售部", "employeeCount", 2L)));
        when(organizationMapper.selectDepartmentPositionCounts("TENANT_001")).thenReturn(List.of());
        when(employeeMapper.countAvailableEmployees("TENANT_001")).thenReturn(2L);

        OrganizationOverviewVO overview = service.overview();

        assertThat(overview.getDepartments().get(0).getEmployeeCount()).isEqualTo(2L);
        assertThat(overview.getStats().getEmptyDepartmentCount()).isZero();
    }

    @Test
    void parentMembersIncludeOnlyItsOwnDescendantsAndKeepPhonesMasked() {
        Department parent = department(1L, null, "总部");
        when(departmentMapper.selectOne(any())).thenReturn(parent);
        when(departmentMapper.selectList(any())).thenReturn(List.of(
                parent,
                department(2L, 1L, "销售部"),
                department(3L, 2L, "华东组"),
                department(4L, null, "财务部")
        ));
        OrganizationEmployeeVO employee = new OrganizationEmployeeVO();
        employee.setName("下级员工");
        employee.setDepartmentName("华东组");
        employee.setPhone("13800001234");
        when(organizationMapper.selectEmployeesByDepartments(eq("TENANT_001"), anyList()))
                .thenReturn(List.of(employee));
        when(privacyProtectionUtil.maskPhone("13800001234")).thenReturn("138****1234");

        assertThat(service.employees(1L)).extracting(OrganizationEmployeeVO::getName).containsExactly("下级员工");
        assertThat(employee.getPhone()).isEqualTo("138****1234");
        verify(organizationMapper).selectEmployeesByDepartments("TENANT_001", List.of("总部", "销售部", "华东组"));
    }

    @Test
    void departmentWithChildrenCannotBeDeletedEvenWhenItHasNoDirectEmployees() {
        when(departmentMapper.selectOne(any())).thenReturn(department(1L, null, "总部"));
        when(departmentMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("下级部门");
        verify(departmentMapper, never()).deleteById(1L);
    }

    @Test
    void departmentWithDirectEmployeesStillCannotBeDeleted() {
        when(departmentMapper.selectOne(any())).thenReturn(department(1L, null, "总部"));
        when(departmentMapper.selectCount(any())).thenReturn(0L);
        when(organizationMapper.selectDepartmentEmployeeCounts("TENANT_001"))
                .thenReturn(List.of(Map.of("departmentName", "总部", "employeeCount", 1L)));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("员工");
        verify(departmentMapper, never()).deleteById(1L);
    }

    private Department department(Long id, Long parentId, String name) {
        Department department = new Department();
        department.setId(id);
        department.setParentId(parentId);
        department.setTenantCode("TENANT_001");
        department.setDeptName(name);
        department.setSortNo(id.intValue());
        department.setStatus(1);
        department.setIsDeleted(0);
        return department;
    }
}
