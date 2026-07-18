package my.hive.domain.auth.service;

import my.hive.domain.auth.model.WechatLoginRequest;
import my.hive.infrastructure.wechat.WechatMiniProgramClient;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.context.TenantContext;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.hive.shared.tenant.BoundedTenantProperties;
import my.hive.domain.auth.mapper.AuthMapper;
import my.hive.domain.auth.model.vo.LoginUserRow;
import my.hive.domain.tenant.service.TenantLicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {
    private final AuthMapper mapper = mock(AuthMapper.class);
    private final WechatMiniProgramClient wechat = mock(WechatMiniProgramClient.class);
    private final PrivacyProtectionUtil privacy = mock(PrivacyProtectionUtil.class);
    private final TenantLicenseService license = mock(TenantLicenseService.class);
    private final BoundedTenantProperties tenants = mock(BoundedTenantProperties.class);
    private final AuthenticationService service = new AuthenticationService();
    private final TenantContext context = mock(TenantContext.class);

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(service, "authMapper", mapper);
        ReflectionTestUtils.setField(service, "wechatMiniProgramClient", wechat);
        ReflectionTestUtils.setField(service, "privacyProtectionUtil", privacy);
        ReflectionTestUtils.setField(service, "tenantLicenseService", license);
        ReflectionTestUtils.setField(service, "boundedTenantProperties", tenants);
        ReflectionTestUtils.setField(service, "tenantContext", context);
        when(tenants.allowedTenantCodes()).thenReturn(List.of("a", "b"));
        when(wechat.getPhoneNumber("code")).thenReturn("13800000000");
        when(privacy.hashPhone("13800000000")).thenReturn("hash");
    }

    @Test void rejectsWechatPhoneSharedByMultipleAllowedTenants() {
        when(mapper.selectLoginUsersByPhoneInTenants("13800000000", "hash", null, List.of("a", "b")))
                .thenReturn(List.of(user(1L, "a", 1), user(2L, "b", 1)));
        WechatLoginRequest request = new WechatLoginRequest(); request.setPhoneCode("code");
        assertThatThrownBy(() -> service.wechatLogin(request)).isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(409, "该手机号属于多个企业，请使用账号密码登录或联系企业负责人处理");
    }

    @Test void returnsChineseMessageWhenWechatTenantIsNotAllowed() {
        when(tenants.isTenantAllowed("blocked")).thenReturn(false);
        WechatLoginRequest request = new WechatLoginRequest();
        request.setPhoneCode("code");
        request.setTenantCode("blocked");

        assertThatThrownBy(() -> service.wechatLogin(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(403, "当前企业不可用，请联系企业负责人");
    }

    @Test void directsUnknownWechatPhoneToJoinOrganizationInsteadOfReportingDisabledAccount() {
        when(mapper.selectLoginUsersByPhoneInTenants("13800000000", "hash", null, List.of("a", "b")))
                .thenReturn(List.of());
        WechatLoginRequest request = new WechatLoginRequest(); request.setPhoneCode("code");

        assertThatThrownBy(() -> service.wechatLogin(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(403, "该手机号尚未加入企业，请先加入组织");
    }

    @Test void rejectsDisabledAndProbationUsesTenantEligibilityPolicy() {
        LoginUserRow disabled = user(1L, "a", -1);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateLoginEligibility", disabled))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(403, "该员工账号已禁用，请联系企业负责人");
        LoginUserRow probation = user(2L, "a", 2);
        ReflectionTestUtils.invokeMethod(service, "validateLoginEligibility", probation);
        verify(license).ensureTenantUsable("a");
    }

    @Test void logoutInvalidatesVersionedSessionsByIncrementingAuthVersion() {
        when(context.userId()).thenReturn(9L); when(context.tenantCode()).thenReturn("a");
        service.logout();
        verify(mapper).incrementAuthVersion(9L, "a");
    }

    @Test void logoutWithoutSessionReturnsChineseAuthenticationMessage() {
        when(context.userId()).thenReturn(null);
        when(context.tenantCode()).thenReturn(null);

        assertThatThrownBy(service::logout)
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(401, "请先登录");
    }

    @Test void accountWithoutTenantReturnsChineseUnavailableMessage() {
        LoginUserRow user = user(3L, null, 1);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateLoginEligibility", user))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(403, "当前企业已停用或不可用，请联系企业负责人");
    }

    private LoginUserRow user(long id, String tenant, int status) { LoginUserRow u=new LoginUserRow(); u.setUserId(id); u.setTenantCode(tenant); u.setUserStatus(status); u.setAuthVersion(1L); return u; }
}
