package my.hive.domain.employee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.domain.employee.mapper.DepartmentMapper;
import my.hive.domain.employee.mapper.EmployeeAttendanceLocationMapper;
import my.hive.domain.employee.mapper.EmployeeChangeLogMapper;
import my.hive.domain.employee.mapper.EmployeeExtMapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.mapper.PositionMapper;
import my.hive.domain.employee.model.dto.EmployeeCreateRequest;
import my.hive.domain.employee.model.entity.Department;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.employee.model.entity.Position;
import my.hive.domain.employee.model.enums.EmployeeStatusEnum;
import my.hive.domain.employee.model.vo.EmployeeCreateVO;
import my.hive.domain.permission.mapper.SysRoleMapper;
import my.hive.domain.permission.mapper.SysUserRoleMapper;
import my.hive.domain.tenant.service.TenantLicenseService;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.enums.CommonStatusEnum;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.hive.shared.utils.CodeGeneratorUtil;
import my.hive.shared.utils.EncryptUtil;
import my.hive.shared.utils.PermissionCacheUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeServiceTest {

    private static final String TENANT_CODE = "TENANT_001";
    private static final String SHARED_DEFAULT_PASSWORD = "Test@123456";

    private final EmployeeService service = new EmployeeService();
    private final EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
    private final EmployeeExtMapper employeeExtMapper = mock(EmployeeExtMapper.class);
    private final DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
    private final PositionMapper positionMapper = mock(PositionMapper.class);
    private final EmployeeChangeLogMapper employeeChangeLogMapper = mock(EmployeeChangeLogMapper.class);
    private final SysRoleMapper sysRoleMapper = mock(SysRoleMapper.class);
    private final SysUserRoleMapper sysUserRoleMapper = mock(SysUserRoleMapper.class);
    private final EmployeeAttendanceLocationMapper employeeAttendanceLocationMapper = mock(EmployeeAttendanceLocationMapper.class);
    private final TenantLicenseService tenantLicenseService = mock(TenantLicenseService.class);
    private final CodeGeneratorUtil codeGeneratorUtil = mock(CodeGeneratorUtil.class);
    private final PermissionCacheUtil permissionCacheUtil = mock(PermissionCacheUtil.class);
    private final PrivacyProtectionUtil privacyProtectionUtil = new PrivacyProtectionUtil();
    private final EncryptUtil encryptUtil = new EncryptUtil();

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init(TENANT_CODE, 7L, Set.of());
        ReflectionTestUtils.setField(privacyProtectionUtil, "hashSecret", "employee-service-test-secret");
        ReflectionTestUtils.setField(service, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(service, "employeeExtMapper", employeeExtMapper);
        ReflectionTestUtils.setField(service, "departmentMapper", departmentMapper);
        ReflectionTestUtils.setField(service, "positionMapper", positionMapper);
        ReflectionTestUtils.setField(service, "employeeChangeLogMapper", employeeChangeLogMapper);
        ReflectionTestUtils.setField(service, "sysRoleMapper", sysRoleMapper);
        ReflectionTestUtils.setField(service, "sysUserRoleMapper", sysUserRoleMapper);
        ReflectionTestUtils.setField(service, "employeeAttendanceLocationMapper", employeeAttendanceLocationMapper);
        ReflectionTestUtils.setField(service, "tenantLicenseService", tenantLicenseService);
        ReflectionTestUtils.setField(service, "codeGeneratorUtil", codeGeneratorUtil);
        ReflectionTestUtils.setField(service, "permissionCacheUtil", permissionCacheUtil);
        ReflectionTestUtils.setField(service, "privacyProtectionUtil", privacyProtectionUtil);
        ReflectionTestUtils.setField(service, "encryptUtil", encryptUtil);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        Department department = new Department();
        department.setId(10L);
        department.setDeptName("研发部");
        department.setStatus(CommonStatusEnum.ENABLED.getCode());
        department.setIsDeleted(0);
        Position position = new Position();
        position.setId(20L);
        position.setDepartmentId(10L);
        position.setPositionName("工程师");
        position.setStatus(CommonStatusEnum.ENABLED.getCode());
        position.setIsDeleted(0);
        when(departmentMapper.selectOne(any())).thenReturn(department);
        when(positionMapper.selectOne(any())).thenReturn(position);
        when(employeeMapper.selectList(any())).thenReturn(List.of());
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());
        when(employeeMapper.incrementPermissionVersion(TENANT_CODE, 100L)).thenReturn(1);
        when(codeGeneratorUtil.generateEmployeeNo()).thenReturn("EMP0001");
        doAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(100L);
            return 1;
        }).when(employeeMapper).insert(any(Employee.class));
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void createProvisionsActivationFirstEmployeeWithPrivateCredential() {
        EmployeeCreateVO result = service.create(createRequest("13800000000"));

        assertThat(result)
                .extracting(EmployeeCreateVO::getEmployeeId,
                        EmployeeCreateVO::getEmpNo,
                        EmployeeCreateVO::getPhoneMask,
                        EmployeeCreateVO::getActivationRequired)
                .containsExactly(100L, "EMP0001", "138****0000", true);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).insert(employeeCaptor.capture());
        Employee stored = employeeCaptor.getValue();
        assertThat(stored.getLoginName()).isEqualTo("EMP0001");
        assertThat(stored.getPhoneHash()).isEqualTo(privacyProtectionUtil.hashPhone("13800000000"));
        assertThat(stored.getPhoneMask()).isEqualTo("138****0000");
        assertThat(stored.getMustChangePassword()).isEqualTo(1);
        assertThat(encryptUtil.isBcryptHash(stored.getPassword())).isTrue();
        assertThat(encryptUtil.matches(SHARED_DEFAULT_PASSWORD, stored.getPassword())).isFalse();
    }

    @Test
    void createRejectsDuplicatePhoneWithoutInsertingAnotherEmployee() {
        Employee existing = new Employee();
        existing.setId(99L);
        when(employeeMapper.selectList(any())).thenReturn(List.of(existing));
        when(employeeExtMapper.selectOne(any())).thenReturn(new my.hive.domain.employee.model.entity.EmployeeExt());

        assertThatThrownBy(() -> service.create(createRequest("13800000000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getReason()).isEqualTo("PHONE_ACCOUNT_AMBIGUOUS");
                    assertThat(exception.getMsg()).contains("手机号");
                });

        verify(employeeMapper, never()).insert(any(Employee.class));
    }

    private EmployeeCreateRequest createRequest(String phone) {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("测试员工");
        request.setPhone(phone);
        request.setDepartmentId(10L);
        request.setPositionId(20L);
        request.setLeaderName("直属主管");
        request.setEntryDate(LocalDate.of(2026, 7, 18));
        request.setStatus(EmployeeStatusEnum.ACTIVE.getCode());
        return request;
    }
}
