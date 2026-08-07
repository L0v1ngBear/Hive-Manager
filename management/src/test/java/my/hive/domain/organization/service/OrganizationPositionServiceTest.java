package my.hive.domain.organization.service;

import my.hive.domain.employee.mapper.DepartmentMapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.mapper.PositionMapper;
import my.hive.domain.employee.model.entity.Department;
import my.hive.domain.employee.model.entity.Position;
import my.hive.domain.organization.mapper.OrganizationMapper;
import my.hive.domain.organization.model.dto.OrganizationPositionSaveRequest;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.utils.CodeGeneratorUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizationPositionServiceTest {

    private final DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
    private final PositionMapper positionMapper = mock(PositionMapper.class);
    private final EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
    private final OrganizationMapper organizationMapper = mock(OrganizationMapper.class);
    private final CodeGeneratorUtil codeGenerator = mock(CodeGeneratorUtil.class);
    private final OrganizationService service = new OrganizationService();

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of("organization:view"));
        ReflectionTestUtils.setField(service, "departmentMapper", departmentMapper);
        ReflectionTestUtils.setField(service, "positionMapper", positionMapper);
        ReflectionTestUtils.setField(service, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(service, "organizationMapper", organizationMapper);
        ReflectionTestUtils.setField(service, "codeGeneratorUtil", codeGenerator);
    }

    @AfterEach
    void clearContext() {
        TenantPermissionContext.clear();
    }

    @Test
    void departmentWithPositionsCannotBeDeleted() {
        Department department = department(10L, "销售部");
        when(departmentMapper.selectOne(any())).thenReturn(department);
        when(departmentMapper.selectCount(any())).thenReturn(0L);
        when(organizationMapper.selectDepartmentEmployeeCounts("TENANT_001")).thenReturn(List.of());
        when(positionMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("职位");

        verify(departmentMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void emptyDepartmentUsesLogicDeleteAndVerifiesAffectedRow() {
        Department department = department(10L, "销售部");
        when(departmentMapper.selectOne(any())).thenReturn(department);
        when(departmentMapper.selectCount(any())).thenReturn(0L);
        when(organizationMapper.selectDepartmentEmployeeCounts("TENANT_001")).thenReturn(List.of());
        when(positionMapper.selectCount(any())).thenReturn(0L);
        when(departmentMapper.deleteById(10L)).thenReturn(1);

        assertThatCode(() -> service.delete(10L)).doesNotThrowAnyException();

        verify(departmentMapper).deleteById(10L);
        verify(departmentMapper, never()).updateById(any());
    }

    @Test
    void departmentDeleteFailsWhenLogicDeleteUpdatesNoRow() {
        Department department = department(10L, "销售部");
        when(departmentMapper.selectOne(any())).thenReturn(department);
        when(departmentMapper.selectCount(any())).thenReturn(0L);
        when(organizationMapper.selectDepartmentEmployeeCounts("TENANT_001")).thenReturn(List.of());
        when(positionMapper.selectCount(any())).thenReturn(0L);
        when(departmentMapper.deleteById(10L)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("删除失败");
    }

    @Test
    void emptyPositionUsesLogicDeleteAndVerifiesAffectedRow() {
        Department department = department(10L, "销售部");
        Position position = position(20L, 10L, "销售专员");
        when(positionMapper.selectOne(any())).thenReturn(position);
        when(departmentMapper.selectOne(any())).thenReturn(department);
        when(organizationMapper.countEmployeesByPosition("TENANT_001", "销售部", "销售专员")).thenReturn(0L);
        when(positionMapper.deleteById(20L)).thenReturn(1);

        assertThatCode(() -> service.deletePosition(20L)).doesNotThrowAnyException();

        verify(positionMapper).deleteById(20L);
        verify(positionMapper, never()).updateById(any());
    }

    @Test
    void renamingPositionUpdatesEmployeesInTheSameDepartment() {
        Department department = department(10L, "销售部");
        Position position = new Position();
        position.setId(20L);
        position.setTenantCode("TENANT_001");
        position.setDepartmentId(10L);
        position.setPositionName("销售专员");
        position.setPositionCode("POS-1");
        position.setStatus(1);
        position.setIsDeleted(0);
        when(departmentMapper.selectOne(any())).thenReturn(department);
        when(positionMapper.selectOne(any())).thenReturn(position);
        when(positionMapper.selectCount(any())).thenReturn(0L);

        OrganizationPositionSaveRequest request = new OrganizationPositionSaveRequest();
        request.setId(20L);
        request.setDepartmentId(10L);
        request.setPositionName("高级销售专员");
        request.setPositionCode("POS-1");
        request.setSortNo(1);
        request.setStatus(0);

        service.savePosition(request);

        verify(positionMapper).updateById(position);
        org.assertj.core.api.Assertions.assertThat(position.getStatus()).isEqualTo(1);
        verify(organizationMapper).updateEmployeePositionName(
                "TENANT_001", "销售部", "销售专员", "高级销售专员");
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setTenantCode("TENANT_001");
        department.setDeptName(name);
        department.setStatus(1);
        department.setIsDeleted(0);
        return department;
    }

    private Position position(Long id, Long departmentId, String name) {
        Position position = new Position();
        position.setId(id);
        position.setTenantCode("TENANT_001");
        position.setDepartmentId(departmentId);
        position.setPositionName(name);
        position.setStatus(1);
        position.setIsDeleted(0);
        return position;
    }
}
