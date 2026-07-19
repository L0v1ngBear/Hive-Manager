package my.hive.domain.tenant.service;

import my.hive.domain.auth.model.AuthReason;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.tenant.mapper.TenantMapper;
import my.hive.domain.tenant.model.entity.Tenant;
import my.hive.domain.tenant.model.enums.TenantPlanEnum;
import my.hive.domain.tenant.model.enums.TenantSubscriptionStatusEnum;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantLicenseServiceTest {

    private final TenantMapper tenantMapper = mock(TenantMapper.class);
    private final EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
    private final TenantLicenseService service = new TenantLicenseService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tenantMapper", tenantMapper);
        ReflectionTestUtils.setField(service, "employeeMapper", employeeMapper);
    }

    @Test
    void missingDeletedAndDisabledTenantUseTenantUnavailableReason() {
        when(tenantMapper.selectOne(any())).thenReturn(null);
        assertTenantUnavailable(() -> service.ensureTenantUsable("missing"));

        Tenant deleted = activeTenant();
        deleted.setDeleted(1);
        when(tenantMapper.selectOne(any())).thenReturn(deleted);
        assertTenantUnavailable(() -> service.ensureTenantUsable("deleted"));

        Tenant disabled = activeTenant();
        disabled.setStatus(0);
        when(tenantMapper.selectOne(any())).thenReturn(disabled);
        assertTenantUnavailable(() -> service.ensureTenantUsable("disabled"));
    }

    @Test
    void subscriptionPackageAndEndDateFailuresUseLicenseUnavailableReason() {
        Tenant expired = activeTenant();
        expired.setSubscriptionStatus(TenantSubscriptionStatusEnum.EXPIRED.getCode());
        when(tenantMapper.selectOne(any())).thenReturn(expired);
        assertLicenseUnavailable(() -> service.ensureTenantUsable("expired"));

        Tenant ended = activeTenant();
        ended.setSubscriptionEndTime(LocalDateTime.now().minusSeconds(1));
        when(tenantMapper.selectOne(any())).thenReturn(ended);
        assertLicenseUnavailable(() -> service.ensureTenantUsable("ended"));

        Tenant unsupportedPackage = activeTenant();
        unsupportedPackage.setPackageCode("UNKNOWN_PACKAGE");
        when(tenantMapper.selectOne(any())).thenReturn(unsupportedPackage);
        assertLicenseUnavailable(() -> service.ensureTenantUsable("unsupported"));
    }

    @Test
    void activeTenantWithCurrentLicenseIsUsable() {
        when(tenantMapper.selectOne(any())).thenReturn(activeTenant());

        assertThatCode(() -> service.ensureTenantUsable("tenant-a")).doesNotThrowAnyException();
    }

    @Test
    void userQuotaFailuresUseLicenseUnavailableReason() {
        Tenant closed = activeTenant();
        closed.setMaxUsers(0);
        when(tenantMapper.selectOne(any())).thenReturn(closed);
        assertThatThrownBy(() -> service.ensureUserQuotaAvailable("tenant-a"))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_LICENSE_UNAVAILABLE,
                        "当前套餐暂未开放员工账号，请升级套餐后再新增员工");

        Tenant full = activeTenant();
        full.setMaxUsers(1);
        when(tenantMapper.selectOne(any())).thenReturn(full);
        when(employeeMapper.countAvailableEmployees("tenant-a")).thenReturn(1L);
        assertThatThrownBy(() -> service.ensureUserQuotaAvailable("tenant-a"))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_LICENSE_UNAVAILABLE,
                        "当前套餐最多允许 1 名员工，请升级套餐后再新增员工");
    }

    private Tenant activeTenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantCode("tenant-a");
        tenant.setStatus(1);
        tenant.setDeleted(0);
        tenant.setPackageCode(TenantPlanEnum.TRIAL.getCode());
        tenant.setSubscriptionStatus(TenantSubscriptionStatusEnum.ACTIVE.getCode());
        tenant.setSubscriptionEndTime(LocalDateTime.now().plusDays(1));
        return tenant;
    }

    private void assertTenantUnavailable(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_UNAVAILABLE,
                        "当前企业已停用或不存在，请联系企业负责人");
    }

    private void assertLicenseUnavailable(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_LICENSE_UNAVAILABLE,
                        "当前企业授权已到期或不可用，请联系平台管理员续费");
    }
}
