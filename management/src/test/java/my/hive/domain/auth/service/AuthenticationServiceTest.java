package my.hive.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.domain.auth.mapper.AuthMapper;
import my.hive.domain.auth.model.AuthReason;
import my.hive.domain.auth.model.WechatLoginRequest;
import my.hive.domain.auth.model.dto.WechatTenantSelectRequest;
import my.hive.domain.auth.model.vo.LoginUserRow;
import my.hive.domain.auth.model.vo.MiniWechatLoginVO;
import my.hive.domain.auth.model.vo.WechatTenantOptionVO;
import my.hive.domain.tenant.service.TenantLicenseService;
import my.hive.infrastructure.wechat.WechatMiniProgramClient;
import my.hive.shared.auth.TokenService;
import my.hive.shared.context.TenantContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.EffectivePermissionService;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.hive.shared.tenant.BoundedTenantProperties;
import my.hive.shared.utils.ResponseEncryptUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {
    private static final String PHONE = "13800000000";
    private static final String HASH = "phone-hash";

    private final AuthMapper mapper = mock(AuthMapper.class);
    private final WechatMiniProgramClient wechat = mock(WechatMiniProgramClient.class);
    private final PrivacyProtectionUtil privacy = mock(PrivacyProtectionUtil.class);
    private final TenantLicenseService license = mock(TenantLicenseService.class);
    private final BoundedTenantProperties tenants = mock(BoundedTenantProperties.class);
    private final EffectivePermissionService permissions = mock(EffectivePermissionService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final ResponseEncryptUtil responseEncryptUtil = mock(ResponseEncryptUtil.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final HiveRedisKeyBuilder redisKeyBuilder = mock(HiveRedisKeyBuilder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthenticationService service = new AuthenticationService();
    private final TenantContext context = mock(TenantContext.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "authMapper", mapper);
        ReflectionTestUtils.setField(service, "wechatMiniProgramClient", wechat);
        ReflectionTestUtils.setField(service, "privacyProtectionUtil", privacy);
        ReflectionTestUtils.setField(service, "tenantLicenseService", license);
        ReflectionTestUtils.setField(service, "boundedTenantProperties", tenants);
        ReflectionTestUtils.setField(service, "tenantContext", context);
        ReflectionTestUtils.setField(service, "effectivePermissionService", permissions);
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "responseEncryptUtil", responseEncryptUtil);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(service, "redisKeyBuilder", redisKeyBuilder);

        when(tenants.allowedTenantCodes()).thenReturn(List.of("a", "b"));
        when(tenants.isTenantAllowed(anyString())).thenReturn(true);
        when(wechat.getPhoneNumber("code")).thenReturn(PHONE);
        when(privacy.hashPhone(PHONE)).thenReturn(HASH);
        when(redis.opsForValue()).thenReturn(values);
        when(redisKeyBuilder.cache(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation ->
                String.join(":",
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)));
        when(permissions.resolve(anyLong(), anyString())).thenReturn(Set.of());
        when(tokenService.create(any(), anyString(), any())).thenReturn("login-token");
        when(responseEncryptUtil.buildResponseKey("login-token")).thenReturn("response-key");
        when(license.enabledFeatureKeys(anyString())).thenReturn(List.of());
    }

    @Test
    void returnsLoggedInFlowForOneEligibleEmployee() {
        when(mapper.selectLoginUsersByPhoneInTenants(PHONE, HASH, null, List.of("a", "b")))
                .thenReturn(List.of(user(1L, "a", 1)));

        MiniWechatLoginVO result = service.wechatLogin(wechatRequest("code"));

        assertThat(result.getFlowStatus()).isEqualTo("LOGGED_IN");
        assertThat(result.getLoginInfo().getTenantCode()).isEqualTo("a");
        assertThat(result.getLoginInfo().getToken()).isEqualTo("login-token");
        assertThat(result.getSelectionTicket()).isNull();
        assertThat(result.getTenants()).isEmpty();
    }

    @Test
    void returnsOneTimeTenantSelectionForMultipleEmployees() throws Exception {
        LoginUserRow tenantA = user(1L, "a", 1);
        tenantA.setTenantName("Tenant A");
        tenantA.setTenantLogoUrl("a.png");
        LoginUserRow tenantB = user(2L, "b", 1);
        tenantB.setTenantName("Tenant B");
        when(mapper.selectLoginUsersByPhoneInTenants(PHONE, HASH, null, List.of("a", "b")))
                .thenReturn(List.of(tenantA, tenantB));

        MiniWechatLoginVO result = service.wechatLogin(wechatRequest("code"));

        assertThat(result.getFlowStatus()).isEqualTo(AuthReason.TENANT_SELECTION_REQUIRED);
        assertThat(result.getSelectionTicket()).isNotBlank();
        assertThat(result.getTenants()).extracting(WechatTenantOptionVO::getTenantCode)
                .containsExactly("a", "b");
        assertThat(result.getTenants()).extracting(WechatTenantOptionVO::getTenantName)
                .containsExactly("Tenant A", "Tenant B");
        assertThat(result.getLoginInfo()).isNull();

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).set(keyCaptor.capture(), jsonCaptor.capture(), eq(5L), eq(TimeUnit.MINUTES));
        assertThat(keyCaptor.getValue())
                .isEqualTo("auth:mini-wechat:tenant-selection:" + result.getSelectionTicket());
        JsonNode stored = objectMapper.readTree(jsonCaptor.getValue());
        assertThat(stored.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("phoneHash", "tenantCodes", "expireAt");
        assertThat(stored.get("phoneHash").asText()).isEqualTo(HASH);
        assertThat(stored.get("tenantCodes")).extracting(JsonNode::asText)
                .containsExactly("a", "b");
        assertThat(jsonCaptor.getValue()).doesNotContain(PHONE, "login-token", "response-key");
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void rejectsUnknownWechatEmployeeWithReasonAndNoLoginToken() {
        when(mapper.selectLoginUsersByPhoneInTenants(PHONE, HASH, null, List.of("a", "b")))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.wechatLogin(wechatRequest("code")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(
                        403,
                        AuthReason.EMPLOYEE_NOT_FOUND,
                        "管理员尚未添加该手机号，请联系企业负责人或使用组织邀请码加入"
                );
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void rejectsExpiredTenantSelectionTicket() throws Exception {
        when(values.getAndDelete(selectionKey("expired")))
                .thenReturn(selectionPayload(HASH, List.of("a", "b"), System.currentTimeMillis() - 1));

        assertInvalidTicket(selectRequest("expired", "a"));
        verify(mapper, never()).selectLoginUsersByPhoneHashAndTenant(anyString(), anyString());
    }

    @Test
    void rejectsTamperedTenantSelectionTicket() {
        when(values.getAndDelete(selectionKey("tampered"))).thenReturn(null);

        assertInvalidTicket(selectRequest("tampered", "a"));
        verify(mapper, never()).selectLoginUsersByPhoneHashAndTenant(anyString(), anyString());
    }

    @Test
    void rejectsTenantSelectionTicketWhenUsedTwice() throws Exception {
        String payload = selectionPayload(HASH, List.of("a", "b"), System.currentTimeMillis() + 60_000);
        when(values.getAndDelete(selectionKey("one-time"))).thenReturn(payload, null);
        when(mapper.selectLoginUsersByPhoneHashAndTenant(HASH, "a"))
                .thenReturn(List.of(user(1L, "a", 1)));

        assertThat(service.selectWechatTenant(selectRequest("one-time", "a")).getToken())
                .isEqualTo("login-token");
        clearInvocations(tokenService);
        assertInvalidTicket(selectRequest("one-time", "a"));
        verify(values, org.mockito.Mockito.times(2)).getAndDelete(selectionKey("one-time"));
    }

    @Test
    void rejectsTenantOutsideTicketCandidateSetWithoutQueryingEmployee() throws Exception {
        when(values.getAndDelete(selectionKey("outside")))
                .thenReturn(selectionPayload(HASH, List.of("a", "b"), System.currentTimeMillis() + 60_000));

        assertThatThrownBy(() -> service.selectWechatTenant(selectRequest("outside", "c")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        verify(mapper, never()).selectLoginUsersByPhoneHashAndTenant(anyString(), anyString());
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void revalidatesSelectedEmployeeAndRejectsMissingEmployeeWithoutToken() throws Exception {
        when(values.getAndDelete(selectionKey("missing")))
                .thenReturn(selectionPayload(HASH, List.of("a"), System.currentTimeMillis() + 60_000));
        when(mapper.selectLoginUsersByPhoneHashAndTenant(HASH, "a")).thenReturn(List.of());

        assertThatThrownBy(() -> service.selectWechatTenant(selectRequest("missing", "a")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason")
                .containsExactly(403, AuthReason.EMPLOYEE_NOT_FOUND);
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void rejectsWechatTenantThatIsNotAllowed() {
        when(tenants.isTenantAllowed("blocked")).thenReturn(false);
        WechatLoginRequest request = wechatRequest("code");
        request.setTenantCode("blocked");

        assertThatThrownBy(() -> service.wechatLogin(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(403, "当前企业不可用，请联系企业负责人");
    }

    @Test
    void rejectsDisabledAndProbationUsesTenantEligibilityPolicy() {
        LoginUserRow disabled = user(1L, "a", -1);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateLoginEligibility", disabled))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(403, "该员工账号已禁用，请联系企业负责人");
        LoginUserRow probation = user(2L, "a", 2);
        ReflectionTestUtils.invokeMethod(service, "validateLoginEligibility", probation);
        verify(license).ensureTenantUsable("a");
    }

    @Test
    void logoutInvalidatesVersionedSessionsByIncrementingAuthVersion() {
        when(context.userId()).thenReturn(9L);
        when(context.tenantCode()).thenReturn("a");
        service.logout();
        verify(mapper).incrementAuthVersion(9L, "a");
    }

    @Test
    void logoutWithoutSessionReturnsChineseAuthenticationMessage() {
        when(context.userId()).thenReturn(null);
        when(context.tenantCode()).thenReturn(null);

        assertThatThrownBy(service::logout)
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(401, "请先登录");
    }

    @Test
    void accountWithoutTenantReturnsChineseUnavailableMessage() {
        LoginUserRow user = user(3L, null, 1);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateLoginEligibility", user))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(403, "当前企业已停用或不可用，请联系企业负责人");
    }

    private void assertInvalidTicket(WechatTenantSelectRequest request) {
        assertThatThrownBy(() -> service.selectWechatTenant(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    private String selectionPayload(String phoneHash, List<String> tenantCodes, long expireAt) throws Exception {
        WechatTenantSelectionPayload payload = new WechatTenantSelectionPayload();
        payload.setPhoneHash(phoneHash);
        payload.setTenantCodes(tenantCodes);
        payload.setExpireAt(expireAt);
        return objectMapper.writeValueAsString(payload);
    }

    private String selectionKey(String ticket) {
        return "auth:mini-wechat:tenant-selection:" + ticket;
    }

    private WechatLoginRequest wechatRequest(String code) {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setPhoneCode(code);
        return request;
    }

    private WechatTenantSelectRequest selectRequest(String ticket, String tenantCode) {
        WechatTenantSelectRequest request = new WechatTenantSelectRequest();
        request.setSelectionTicket(ticket);
        request.setTenantCode(tenantCode);
        return request;
    }

    private LoginUserRow user(long id, String tenant, int status) {
        LoginUserRow user = new LoginUserRow();
        user.setUserId(id);
        user.setTenantCode(tenant);
        user.setUserStatus(status);
        user.setAuthVersion(1L);
        return user;
    }
}
