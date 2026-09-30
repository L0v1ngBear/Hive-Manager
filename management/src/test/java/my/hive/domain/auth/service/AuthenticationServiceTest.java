package my.hive.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.domain.auth.mapper.AuthMapper;
import my.hive.domain.auth.model.AuthReason;
import my.hive.domain.auth.model.WechatLoginRequest;
import my.hive.domain.auth.model.dto.OrganizationJoinRequest;
import my.hive.domain.auth.model.dto.PasswordChangeRequest;
import my.hive.domain.auth.model.dto.WebWechatBindRequest;
import my.hive.domain.auth.model.dto.WebWechatCompleteRequest;
import my.hive.domain.auth.model.dto.WechatTenantSelectRequest;
import my.hive.domain.auth.model.vo.LoginVO;
import my.hive.domain.auth.model.vo.LoginUserRow;
import my.hive.domain.auth.model.vo.MiniWechatLoginVO;
import my.hive.domain.auth.model.vo.WechatTenantOptionVO;
import my.hive.domain.auth.model.vo.WebWechatLoginVO;
import my.hive.domain.employee.mapper.DepartmentMapper;
import my.hive.domain.employee.mapper.EmployeeExtMapper;
import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.mapper.PositionMapper;
import my.hive.domain.employee.model.entity.Department;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.employee.model.entity.Position;
import my.hive.domain.permission.mapper.SysUserRoleMapper;
import my.hive.domain.permission.model.entity.SysRole;
import my.hive.domain.permission.service.BuiltInRoleProvisionService;
import my.hive.domain.organization.model.OrganizationInvitationPayload;
import my.hive.domain.organization.service.OrganizationInvitationService;
import my.hive.domain.tenant.mapper.TenantMapper;
import my.hive.domain.tenant.model.entity.Tenant;
import my.hive.domain.tenant.service.TenantLicenseService;
import my.hive.infrastructure.wechat.WechatMiniProgramClient;
import my.hive.infrastructure.wechat.WechatWebLoginClient;
import my.hive.shared.auth.TokenService;
import my.hive.shared.context.TenantContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.EffectivePermissionService;
import my.hive.shared.privacy.PrivacyProtectionUtil;
import my.hive.shared.redis.HiveRedisKeyBuilder;
import my.hive.shared.tenant.BoundedTenantProperties;
import my.hive.shared.utils.CodeGeneratorUtil;
import my.hive.shared.utils.EncryptUtil;
import my.hive.shared.utils.PermissionCacheUtil;
import my.hive.shared.utils.ResponseEncryptUtil;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {
    private static final String PHONE = "13800000000";
    private static final String HASH = "phone-hash";
    private static final String MASK = "138****0000";

    private final AuthMapper mapper = mock(AuthMapper.class);
    private final WechatMiniProgramClient wechat = mock(WechatMiniProgramClient.class);
    private final WechatWebLoginClient webWechat = mock(WechatWebLoginClient.class);
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
    private final EncryptUtil encryptUtil = mock(EncryptUtil.class);
    private final PermissionCacheUtil permissionCacheUtil = mock(PermissionCacheUtil.class);
    private final TenantMapper tenantMapper = mock(TenantMapper.class);
    private final EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
    private final EmployeeExtMapper employeeExtMapper = mock(EmployeeExtMapper.class);
    private final DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
    private final PositionMapper positionMapper = mock(PositionMapper.class);
    private final SysUserRoleMapper sysUserRoleMapper = mock(SysUserRoleMapper.class);
    private final BuiltInRoleProvisionService builtInRoleProvisionService = mock(BuiltInRoleProvisionService.class);
    private final CodeGeneratorUtil codeGeneratorUtil = mock(CodeGeneratorUtil.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthenticationService service = new AuthenticationService();
    private final TenantContext context = mock(TenantContext.class);
    private final PublicAuthRateLimiter publicAuthRateLimiter = mock(PublicAuthRateLimiter.class);
    private final OrganizationInvitationService organizationInvitationService = mock(OrganizationInvitationService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "authMapper", mapper);
        ReflectionTestUtils.setField(service, "wechatMiniProgramClient", wechat);
        ReflectionTestUtils.setField(service, "wechatWebLoginClient", webWechat);
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
        ReflectionTestUtils.setField(service, "encryptUtil", encryptUtil);
        ReflectionTestUtils.setField(service, "permissionCacheUtil", permissionCacheUtil);
        ReflectionTestUtils.setField(service, "tenantMapper", tenantMapper);
        ReflectionTestUtils.setField(service, "employeeMapper", employeeMapper);
        ReflectionTestUtils.setField(service, "employeeExtMapper", employeeExtMapper);
        ReflectionTestUtils.setField(service, "departmentMapper", departmentMapper);
        ReflectionTestUtils.setField(service, "positionMapper", positionMapper);
        ReflectionTestUtils.setField(service, "sysUserRoleMapper", sysUserRoleMapper);
        ReflectionTestUtils.setField(service, "builtInRoleProvisionService", builtInRoleProvisionService);
        ReflectionTestUtils.setField(service, "codeGeneratorUtil", codeGeneratorUtil);
        ReflectionTestUtils.setField(service, "publicAuthRateLimiter", publicAuthRateLimiter);
        ReflectionTestUtils.setField(service, "organizationInvitationService", organizationInvitationService);

        when(tenants.allowedTenantCodes()).thenReturn(List.of("a", "b"));
        when(tenants.isTenantAllowed(anyString())).thenReturn(true);
        when(wechat.getPhoneNumber("code")).thenReturn(PHONE);
        when(privacy.normalizePhone(PHONE)).thenReturn(PHONE);
        when(privacy.hashPhone(PHONE)).thenReturn(HASH);
        when(privacy.maskPhone(PHONE)).thenReturn(MASK);
        when(redis.opsForValue()).thenReturn(values);
        when(redisKeyBuilder.cache(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation ->
                String.join(":",
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)));
        when(redisKeyBuilder.cache(anyString(), anyString(), anyString())).thenAnswer(invocation ->
                String.join(":",
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2)));
        when(redisKeyBuilder.counter(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation ->
                String.join(":",
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)));
        when(permissions.resolve(anyLong(), anyString())).thenReturn(Set.of());
        when(tokenService.create(any(), anyString(), any())).thenReturn("login-token");
        when(responseEncryptUtil.buildResponseKey("login-token")).thenReturn("response-key");
        when(encryptUtil.encode(anyString())).thenReturn("encoded-password");
        when(license.enabledFeatureKeys(anyString())).thenReturn(List.of());
        when(mapper.backfillWechatPhoneHashAndMask(anyLong(), anyString(), eq(PHONE), eq(HASH), eq(MASK)))
                .thenReturn(1);
    }

    @Test
    void completesBoundWebWechatIdentityThroughExistingLoginTokenFlow() throws Exception {
        WebWechatIdentityPayload payload = new WebWechatIdentityPayload();
        payload.setSubjectHash("wechat-subject-hash");
        payload.setExpireAt(System.currentTimeMillis() + 60_000);
        when(values.getAndDelete("auth:web-wechat:login:login-ticket"))
                .thenReturn(objectMapper.writeValueAsString(payload));
        LoginUserRow loginUser = user(7L, "a", 1);
        when(mapper.selectWebWechatLoginUsersBySubjectHashInTenants(
                "wechat-subject-hash", List.of("a", "b"))).thenReturn(List.of(loginUser));
        WebWechatCompleteRequest request = new WebWechatCompleteRequest();
        request.setLoginTicket("login-ticket");

        WebWechatLoginVO result = service.completeWebWechatLogin(request, "203.0.113.7");

        assertThat(result.getFlowStatus()).isEqualTo("LOGGED_IN");
        assertThat(result.getLoginInfo().getToken()).isEqualTo("login-token");
        verify(mapper).selectWebWechatLoginUsersBySubjectHashInTenants(
                "wechat-subject-hash", List.of("a", "b"));
    }

    @Test
    void firstWebWechatLoginRequiresOneTimeExistingAccountBinding() throws Exception {
        WebWechatIdentityPayload payload = new WebWechatIdentityPayload();
        payload.setSubjectHash("wechat-subject-hash");
        payload.setExpireAt(System.currentTimeMillis() + 60_000);
        when(values.getAndDelete("auth:web-wechat:login:login-ticket"))
                .thenReturn(objectMapper.writeValueAsString(payload));
        when(mapper.selectWebWechatLoginUsersBySubjectHashInTenants(
                "wechat-subject-hash", List.of("a", "b"))).thenReturn(List.of());
        WebWechatCompleteRequest request = new WebWechatCompleteRequest();
        request.setLoginTicket("login-ticket");

        WebWechatLoginVO result = service.completeWebWechatLogin(request, "203.0.113.7");

        assertThat(result.getFlowStatus()).isEqualTo("BIND_REQUIRED");
        assertThat(result.getBindingTicket()).isNotBlank();
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void bindsVerifiedAccountWithoutPersistingRawWechatIdentity() throws Exception {
        WebWechatIdentityPayload payload = new WebWechatIdentityPayload();
        payload.setSubjectHash("wechat-subject-hash");
        payload.setExpireAt(System.currentTimeMillis() + 60_000);
        when(values.get("auth:web-wechat:binding:binding-ticket"))
                .thenReturn(objectMapper.writeValueAsString(payload));
        LoginUserRow loginUser = user(7L, "a", 1);
        loginUser.setPassword("encoded-password");
        when(mapper.selectLoginUsers("alice", null, List.of("a", "b"))).thenReturn(List.of(loginUser));
        when(encryptUtil.matches("Password1", "encoded-password")).thenReturn(true);
        when(mapper.selectWebWechatIdentityUserId("wechat-subject-hash", "a")).thenReturn(null);
        when(mapper.selectWebWechatIdentitySubjectHash(7L, "a")).thenReturn(null);
        when(mapper.insertWebWechatIdentity("a", 7L, "wechat-subject-hash")).thenReturn(1);
        WebWechatBindRequest request = new WebWechatBindRequest();
        request.setBindingTicket("binding-ticket");
        request.setUsername("alice");
        request.setPassword("Password1");

        WebWechatLoginVO result = service.bindWebWechatLogin(request, "203.0.113.7");

        assertThat(result.getFlowStatus()).isEqualTo("LOGGED_IN");
        verify(mapper).insertWebWechatIdentity("a", 7L, "wechat-subject-hash");
        verify(redis).delete("auth:web-wechat:binding:binding-ticket");
    }

    @Test
    void returnsLoggedInFlowForOneEligibleEmployee() {
        mockWechatCandidates(List.of(user(1L, "a", 1)));

        MiniWechatLoginVO result = service.wechatLogin(wechatRequest("code"));

        assertThat(result.getFlowStatus()).isEqualTo("LOGGED_IN");
        assertThat(result.getLoginInfo().getTenantCode()).isEqualTo("a");
        assertThat(result.getLoginInfo().getToken()).isEqualTo("login-token");
        assertThat(result.getSelectionTicket()).isNull();
        assertThat(result.getTenants()).isEmpty();
    }

    @Test
    void backfillsMultipleLegacyTenantsIncludingPlainShaSeedBeforeOneTimeSelection() throws Exception {
        LoginUserRow tenantA = user(1L, "a", 1);
        tenantA.setTenantName("Tenant A");
        tenantA.setTenantLogoUrl("a.png");
        LoginUserRow tenantB = user(2L, "b", 1);
        tenantB.setTenantName("Tenant B");
        mockWechatCandidates(List.of(tenantA, tenantB));

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
        verify(mapper).backfillWechatPhoneHashAndMask(1L, "a", PHONE, HASH, MASK);
        verify(mapper).backfillWechatPhoneHashAndMask(2L, "b", PHONE, HASH, MASK);

        when(values.getAndDelete(keyCaptor.getValue())).thenReturn(jsonCaptor.getValue());
        when(mapper.selectLoginUsersByPhoneHashAndTenant(HASH, "a")).thenReturn(List.of(tenantA));
        assertThat(service.selectWechatTenant(selectRequest(result.getSelectionTicket(), "a")).getToken())
                .isEqualTo("login-token");
        verify(mapper).selectLoginUsersByPhoneHashAndTenant(HASH, "a");
    }

    @Test
    void rejectsUnknownWechatEmployeeWithOneTimePhoneProofAndNoLoginToken() throws Exception {
        mockWechatCandidates(List.of());

        long before = System.currentTimeMillis();
        BusinessException exception = catchThrowableOfType(
                () -> service.wechatLogin(wechatRequest("code")),
                BusinessException.class
        );
        long after = System.currentTimeMillis();

        assertThat(exception)
                .extracting("code", "reason", "msg")
                .containsExactly(
                        403,
                        AuthReason.EMPLOYEE_NOT_FOUND,
                        "管理员尚未添加该手机号，请联系企业负责人或使用组织邀请码加入"
                );
        assertThat(exception.getData()).asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("phoneVerificationTicket");
        String ticket = (String) ((Map<?, ?>) exception.getData())
                .get("phoneVerificationTicket");
        assertThat(ticket).isNotBlank();

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).set(keyCaptor.capture(), jsonCaptor.capture(), eq(5L), eq(TimeUnit.MINUTES));
        assertThat(keyCaptor.getValue()).isEqualTo("auth:mini-wechat:phone-proof:" + ticket);
        JsonNode stored = objectMapper.readTree(jsonCaptor.getValue());
        assertThat(stored.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("phoneHash", "expireAt");
        assertThat(stored.get("phoneHash").asText()).isEqualTo(HASH);
        assertThat(stored.get("expireAt").asLong())
                .isBetween(before + TimeUnit.MINUTES.toMillis(5),
                        after + TimeUnit.MINUTES.toMillis(5));
        assertThat(jsonCaptor.getValue()).doesNotContain(PHONE, "code", ticket);
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void joinsWithMatchingWechatPhoneProofAndRejectsReuse() throws Exception {
        prepareSuccessfulOrganizationJoin();
        when(values.getAndDelete(phoneProofKey("proof-1")))
                .thenReturn(phoneProofPayload(HASH, System.currentTimeMillis() + 60_000), null);

        LoginVO result = service.joinOrganization(joinRequest("proof-1", null));

        assertThat(result.getToken()).isEqualTo("login-token");
        verify(values, never()).get(organizationJoinSmsKey());
        assertInvalidPhoneProof(() -> service.joinOrganization(joinRequest("proof-1", null)));
        verify(values, org.mockito.Mockito.times(2)).getAndDelete(phoneProofKey("proof-1"));
    }

    @Test
    void rejectsWechatPhoneProofForDifferentTypedPhoneHash() throws Exception {
        when(values.getAndDelete(phoneProofKey("mismatch")))
                .thenReturn(phoneProofPayload("different-phone-hash", System.currentTimeMillis() + 60_000));

        assertInvalidPhoneProof(() -> service.joinOrganization(joinRequest("mismatch", null)));

        verify(values).getAndDelete(phoneProofKey("mismatch"));
        verify(values, never()).get(organizationJoinSmsKey());
    }

    @Test
    void rejectsExpiredWechatPhoneProof() throws Exception {
        when(values.getAndDelete(phoneProofKey("expired-proof")))
                .thenReturn(phoneProofPayload(HASH, System.currentTimeMillis() - 1));

        assertInvalidPhoneProof(() -> service.joinOrganization(joinRequest("expired-proof", null)));

        verify(values).getAndDelete(phoneProofKey("expired-proof"));
    }

    @Test
    void rejectsMalformedWechatPhoneProofWithChineseGuidance() {
        when(values.getAndDelete(phoneProofKey("malformed-proof"))).thenReturn("null");

        assertInvalidPhoneProof(() -> service.joinOrganization(joinRequest("malformed-proof", null)));

        verify(values).getAndDelete(phoneProofKey("malformed-proof"));
    }

    @Test
    void requiresExactlyOneOrganizationJoinPhoneProof() {
        assertThatThrownBy(() -> service.joinOrganization(joinRequest(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(400, "请使用微信手机号验证凭证或短信验证码完成手机号验证");

        assertThatThrownBy(() -> service.joinOrganization(joinRequest("proof", "123456")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(400, "微信手机号验证凭证和短信验证码只能选择一种");
        verify(values, never()).getAndDelete(anyString());
    }

    @Test
    void preservesSmsFallbackForOrganizationJoin() {
        prepareSuccessfulOrganizationJoin();
        when(values.get(organizationJoinSmsKey())).thenReturn("123456");

        LoginVO result = service.joinOrganization(joinRequest(null, "123456"));

        assertThat(result.getToken()).isEqualTo("login-token");
        verify(values).get(organizationJoinSmsKey());
        verify(values, never()).getAndDelete(anyString());
    }

    @Test
    void currentUserPublishesTheEmployeePositionFromTheUserRecord() {
        LoginUserRow loginUser = user(7L, "a", 1);
        loginUser.setUserName("企业管理员");
        loginUser.setPositionName("  运营管理  ");
        when(context.userId()).thenReturn(7L);
        when(context.tenantCode()).thenReturn("a");
        when(mapper.selectLoginUserByUserIdAndTenantCode(7L, "a")).thenReturn(loginUser);

        LoginVO result = service.currentUser();

        assertThat(result.getUserName()).isEqualTo("企业管理员");
        assertThat(result.getPositionName()).isEqualTo("运营管理");
        assertThat(result.getToken()).isNull();
        assertThat(result.getResponseKey()).isNull();
    }

    @Test
    void currentUserLeavesPositionNullWhenTheEmployeeHasNoPosition() {
        LoginUserRow loginUser = user(7L, "a", 1);
        loginUser.setUserName("企业管理员");
        loginUser.setPositionName("   ");
        when(context.userId()).thenReturn(7L);
        when(context.tenantCode()).thenReturn("a");
        when(mapper.selectLoginUserByUserIdAndTenantCode(7L, "a")).thenReturn(loginUser);

        assertThat(service.currentUser().getPositionName()).isNull();
    }

    @Test
    void backfillsLegacyEmployeeBeforeHashOnlyRequery() {
        LoginUserRow legacy = user(7L, "a", 1);
        mockWechatCandidates(List.of(legacy));

        MiniWechatLoginVO result = service.wechatLogin(wechatRequest("code"));

        assertThat(result.getFlowStatus()).isEqualTo("LOGGED_IN");
        verify(mapper).selectWechatLoginUsersByPhoneInTenants(PHONE, HASH, List.of("a", "b"));
        verify(mapper).backfillWechatPhoneHashAndMask(7L, "a", PHONE, HASH, MASK);
        verify(mapper).selectLoginUsersByPhoneHashInTenants(HASH, List.of("a", "b"));
    }

    @Test
    void compatibilityMapperMasksProjectionAndReplacesNonHmacHashesOnlyForUnchangedPhone() throws Exception {
        Select lookup = AuthMapper.class.getMethod(
                        "selectWechatLoginUsersByPhoneInTenants", String.class, String.class, List.class)
                .getAnnotation(Select.class);
        Update backfill = AuthMapper.class.getMethod(
                        "backfillWechatPhoneHashAndMask",
                        Long.class, String.class, String.class, String.class, String.class)
                .getAnnotation(Update.class);
        String lookupSql = String.join(" ", lookup.value());
        String backfillSql = String.join(" ", backfill.value());

        assertThat(lookupSql)
                .contains("u.phone_mask AS phone")
                .contains("u.phone_hash = #{phoneHash} OR u.phone = #{phone}")
                .doesNotContain("COALESCE(u.phone_mask, u.phone)");
        assertThat(backfillSql)
                .contains("phone_hash = #{phoneHash}")
                .contains("phone = #{phone}")
                .contains("phone = NULL")
                .contains("phone_mask = CASE")
                .doesNotContain("phone_hash IS NULL");
    }

    @Test
    void disabledEmployeeReasonPropagatesThroughInitialAndSelectedFlows() throws Exception {
        LoginUserRow disabled = user(1L, "a", -1);
        mockWechatCandidates(List.of(disabled));

        assertThatThrownBy(() -> service.wechatLogin(wechatRequest("code")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.ACCOUNT_DISABLED,
                        "该员工账号已禁用，请联系企业负责人");

        prepareSelection("disabled", disabled);
        assertThatThrownBy(() -> service.selectWechatTenant(selectRequest("disabled", "a")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.ACCOUNT_DISABLED,
                        "该员工账号已禁用，请联系企业负责人");
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void resignedEmployeeReasonPropagatesThroughInitialAndSelectedFlows() throws Exception {
        LoginUserRow resigned = user(1L, "a", 0);
        mockWechatCandidates(List.of(resigned));

        assertThatThrownBy(() -> service.wechatLogin(wechatRequest("code")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.EMPLOYEE_RESIGNED,
                        "该员工账号已离职，请联系管理员重新启用");

        prepareSelection("resigned", resigned);
        assertThatThrownBy(() -> service.selectWechatTenant(selectRequest("resigned", "a")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.EMPLOYEE_RESIGNED,
                        "该员工账号已离职，请联系管理员重新启用");
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void unavailableTenantReasonPropagatesThroughInitialAndSelectedFlows() throws Exception {
        when(tenants.isTenantAllowed("blocked")).thenReturn(false);
        WechatLoginRequest initial = wechatRequest("code");
        initial.setTenantCode("blocked");

        assertThatThrownBy(() -> service.wechatLogin(initial))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_UNAVAILABLE,
                        "当前企业已停用或不可用，请联系企业负责人");

        LoginUserRow active = user(1L, "a", 1);
        prepareSelection("tenant-unavailable", active);
        doThrow(new IllegalArgumentException("tenant is not in allowed tenant list"))
                .when(tenants).assertTenantAllowed("a");
        assertThatThrownBy(() -> service.selectWechatTenant(selectRequest("tenant-unavailable", "a")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_UNAVAILABLE,
                        "当前企业已停用或不可用，请联系企业负责人");
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void unavailableLicenseReasonPropagatesThroughInitialAndSelectedFlows() throws Exception {
        LoginUserRow active = user(1L, "a", 1);
        mockWechatCandidates(List.of(active));
        doThrow(new BusinessException(403, "租户已到期或被停用，请联系平台管理员续费"))
                .when(license).ensureTenantUsable("a");

        assertThatThrownBy(() -> service.wechatLogin(wechatRequest("code")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_LICENSE_UNAVAILABLE,
                        "租户已到期或被停用，请联系平台管理员续费");

        prepareSelection("license-unavailable", active);
        assertThatThrownBy(() -> service.selectWechatTenant(selectRequest("license-unavailable", "a")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_LICENSE_UNAVAILABLE,
                        "租户已到期或被停用，请联系平台管理员续费");
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    @Test
    void preservesReasonAwareTenantUnavailableFailureFromLicenseService() {
        LoginUserRow active = user(1L, "a", 1);
        mockWechatCandidates(List.of(active));
        doThrow(new BusinessException(
                403,
                AuthReason.TENANT_UNAVAILABLE,
                "当前企业已停用或不存在，请联系企业负责人"
        )).when(license).ensureTenantUsable("a");

        assertThatThrownBy(() -> service.wechatLogin(wechatRequest("code")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_UNAVAILABLE,
                        "当前企业已停用或不存在，请联系企业负责人");
    }

    @Test
    void joinsTargetTenantWhenSamePhoneAlreadyBelongsToAnotherTenant() {
        prepareSuccessfulOrganizationJoin("b");
        Employee tenantA = new Employee();
        tenantA.setId(88L);
        tenantA.setTenantCode("a");
        when(employeeMapper.selectOrganizationJoinCandidates("b", HASH, PHONE)).thenReturn(List.of(tenantA));
        when(values.get(organizationJoinSmsKey())).thenReturn("123456");

        LoginVO result = service.joinOrganization(joinRequest(null, "123456"));

        assertThat(result.getTenantCode()).isEqualTo("b");
        verify(employeeMapper).insert(org.mockito.ArgumentMatchers.argThat(employee ->
                "b".equals(employee.getTenantCode()) && HASH.equals(employee.getPhoneHash())));
        verify(license).ensureUserQuotaAvailable("b");
    }

    @Test
    void rejectsDuplicateOnlyInsideInvitationTargetTenant() {
        prepareSuccessfulOrganizationJoin("b");
        Employee tenantB = new Employee();
        tenantB.setId(89L);
        tenantB.setTenantCode("b");
        when(employeeMapper.selectOrganizationJoinCandidates("b", HASH, PHONE)).thenReturn(List.of(tenantB));
        when(values.get(organizationJoinSmsKey())).thenReturn("123456");

        assertThatThrownBy(() -> service.joinOrganization(joinRequest(null, "123456")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(409, "该手机号已加入组织，请直接登录或联系管理员重置密码");
        verify(employeeMapper, never()).insert(any(Employee.class));
    }

    @Test
    void safelyReusesSingleTenantlessLegacyRowForInvitationTarget() {
        prepareSuccessfulOrganizationJoin("b");
        Employee tenantless = new Employee();
        tenantless.setId(77L);
        tenantless.setTenantCode(null);
        when(employeeMapper.selectOrganizationJoinCandidates("b", HASH, PHONE)).thenReturn(List.of(tenantless));
        when(values.get(organizationJoinSmsKey())).thenReturn("123456");
        when(mapper.selectLoginUserByUserIdAndTenantCode(77L, "b")).thenReturn(user(77L, "b", 1));

        LoginVO result = service.joinOrganization(joinRequest(null, "123456"));

        assertThat(result.getTenantCode()).isEqualTo("b");
        verify(employeeMapper).updateById(org.mockito.ArgumentMatchers.argThat(employee ->
                employee.getId().equals(77L) && "b".equals(employee.getTenantCode())));
        verify(employeeMapper, never()).insert(any(Employee.class));
        verify(employeeMapper).incrementPermissionAndAuthVersion("b", 77L);
    }

    @Test
    void organizationJoinCandidateSqlExcludesUnrelatedTenantRows() throws Exception {
        Select select = EmployeeMapper.class.getMethod(
                        "selectOrganizationJoinCandidates", String.class, String.class, String.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ");

        assertThat(sql)
                .contains("tenant_code = #{tenantCode}")
                .contains("tenant_code IS NULL", "tenant_code = ''")
                .contains("phone_hash = #{phoneHash}", "phone = #{phone}")
                .doesNotContain("tenant_code <> #{tenantCode}");
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
                .extracting("code", "reason", "msg")
                .containsExactly(403, AuthReason.TENANT_UNAVAILABLE,
                        "当前企业已停用或不可用，请联系企业负责人");
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
    void changesAuthenticatedPasswordAndRevokesExistingSessions() {
        when(context.userId()).thenReturn(9L);
        when(context.tenantCode()).thenReturn("a");
        LoginUserRow loginUser = user(9L, "a", 1);
        loginUser.setPassword("encoded-old");
        when(mapper.selectLoginUserByUserIdAndTenantCode(9L, "a")).thenReturn(loginUser);
        when(encryptUtil.matches("OldPass1", "encoded-old")).thenReturn(true);
        when(encryptUtil.matches("NewPass2", "encoded-old")).thenReturn(false);
        when(mapper.updatePasswordAndAuthVersionByUserIdAndTenantCode(9L, "a", "encoded-password"))
                .thenReturn(1);

        service.changePassword(passwordChangeRequest("OldPass1", "NewPass2", "NewPass2"));

        verify(mapper).updatePasswordAndAuthVersionByUserIdAndTenantCode(9L, "a", "encoded-password");
        verify(mapper, never()).updatePasswordByUserIdAndTenantCode(anyLong(), anyString(), anyString());
        verify(permissionCacheUtil).evict("a", 9L);
    }

    @Test
    void rejectsAuthenticatedPasswordChangeWhenOldPasswordIsWrong() {
        when(context.userId()).thenReturn(9L);
        when(context.tenantCode()).thenReturn("a");
        LoginUserRow loginUser = user(9L, "a", 1);
        loginUser.setPassword("encoded-old");
        when(mapper.selectLoginUserByUserIdAndTenantCode(9L, "a")).thenReturn(loginUser);
        when(encryptUtil.matches("WrongPass1", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(
                passwordChangeRequest("WrongPass1", "NewPass2", "NewPass2")))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "msg")
                .containsExactly(400, "原密码不正确");

        verify(mapper, never()).updatePasswordAndAuthVersionByUserIdAndTenantCode(
                anyLong(), anyString(), anyString());
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
                .extracting("code", "reason", "msg")
                .containsExactly(400, AuthReason.TENANT_SELECTION_INVALID_OR_EXPIRED,
                        "企业选择凭证无效或已过期，请重新登录");
        verify(tokenService, never()).create(any(), anyString(), any());
    }

    private void assertInvalidPhoneProof(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("code", "reason", "msg")
                .containsExactly(
                        400,
                        AuthReason.INVITATION_INVALID_OR_EXPIRED,
                        "微信手机号验证凭证无效或已过期，请重新授权手机号或使用短信验证码"
                );
    }

    private String phoneProofPayload(String phoneHash, long expireAt) throws Exception {
        WechatPhoneVerificationPayload payload = new WechatPhoneVerificationPayload();
        payload.setPhoneHash(phoneHash);
        payload.setExpireAt(expireAt);
        return objectMapper.writeValueAsString(payload);
    }

    private void prepareSuccessfulOrganizationJoin() {
        prepareSuccessfulOrganizationJoin("a");
    }

    private void prepareSuccessfulOrganizationJoin(String targetTenantCode) {
        OrganizationInvitationPayload invitation = new OrganizationInvitationPayload();
        invitation.setTenantCode(targetTenantCode);
        invitation.setIssuerUserId(7L);
        invitation.setExpiresAt(System.currentTimeMillis() + 60_000);
        invitation.setRemainingUses(0);
        when(organizationInvitationService.consume("JOIN1234")).thenReturn(invitation);
        Tenant tenant = new Tenant();
        tenant.setTenantCode(targetTenantCode);
        tenant.setStatus(1);
        when(tenantMapper.selectByTenantCode(targetTenantCode)).thenReturn(tenant);
        when(employeeMapper.selectOrganizationJoinCandidates(targetTenantCode, HASH, PHONE)).thenReturn(List.of());

        Department department = new Department();
        department.setId(21L);
        department.setDeptName("待分配部门");
        when(departmentMapper.selectOne(any())).thenReturn(department);
        Position position = new Position();
        position.setId(31L);
        position.setPositionName("普通员工");
        when(positionMapper.selectOne(any())).thenReturn(position);
        SysRole role = new SysRole();
        role.setId(41L);
        when(builtInRoleProvisionService.ensureTenantRoles(targetTenantCode))
                .thenReturn(Map.of("EMPLOYEE", role));
        when(employeeMapper.insert(any(Employee.class))).thenAnswer(invocation -> {
            invocation.<Employee>getArgument(0).setId(11L);
            return 1;
        });
        when(employeeExtMapper.insert(any())).thenReturn(1);
        when(sysUserRoleMapper.insert(any())).thenReturn(1);
        when(mapper.selectLoginUserByUserIdAndTenantCode(11L, targetTenantCode))
                .thenReturn(user(11L, targetTenantCode, 1));
    }

    private OrganizationJoinRequest joinRequest(String ticket, String smsCode) {
        OrganizationJoinRequest request = new OrganizationJoinRequest();
        request.setName("张三");
        request.setPhone(PHONE);
        request.setSmsCode(smsCode);
        request.setOrganizationCode("JOIN1234");
        request.setPassword("Password1");
        request.setConfirmPassword("Password1");
        if (ticket != null) {
            request.setPhoneVerificationTicket(ticket);
        }
        return request;
    }

    private String selectionPayload(String phoneHash, List<String> tenantCodes, long expireAt) throws Exception {
        WechatTenantSelectionPayload payload = new WechatTenantSelectionPayload();
        payload.setPhoneHash(phoneHash);
        payload.setTenantCodes(tenantCodes);
        payload.setExpireAt(expireAt);
        return objectMapper.writeValueAsString(payload);
    }

    private void prepareSelection(String ticket, LoginUserRow loginUser) throws Exception {
        when(values.getAndDelete(selectionKey(ticket)))
                .thenReturn(selectionPayload(HASH, List.of("a"), System.currentTimeMillis() + 60_000));
        when(mapper.selectLoginUsersByPhoneHashAndTenant(HASH, "a"))
                .thenReturn(List.of(loginUser));
    }

    private void mockWechatCandidates(List<LoginUserRow> candidates) {
        when(mapper.selectWechatLoginUsersByPhoneInTenants(PHONE, HASH, List.of("a", "b")))
                .thenReturn(candidates);
        when(mapper.selectLoginUsersByPhoneHashInTenants(HASH, List.of("a", "b")))
                .thenReturn(candidates);
    }

    private String selectionKey(String ticket) {
        return "auth:mini-wechat:tenant-selection:" + ticket;
    }

    private String phoneProofKey(String ticket) {
        return "auth:mini-wechat:phone-proof:" + ticket;
    }

    private String organizationJoinSmsKey() {
        return "auth:organization-join:sms:" + HASH;
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

    private PasswordChangeRequest passwordChangeRequest(String oldPassword,
                                                        String newPassword,
                                                        String confirmPassword) {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
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
