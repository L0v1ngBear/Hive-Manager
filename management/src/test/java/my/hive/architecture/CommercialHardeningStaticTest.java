package my.hive.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommercialHardeningStaticTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main", "java");
    private static final Pattern SELECT_STAR = Pattern.compile("(?is)SELECT\\s+\\*\\s+FROM");
    private static final Pattern RAW_SMS_PHONE_LOG = Pattern.compile("(?s)log\\.(info|debug|warn|error)\\([^;]*(?<!maskPhone\\()message\\.phone\\(\\)");
    private static final Pattern RAW_SMS_CONTENT_LOG = Pattern.compile("(?s)log\\.(info|debug|warn|error)\\([^;]*(?<!textLength\\()message\\.content\\(\\)");
    private static final Pattern SENSITIVE_OPERATION_BIZ_NO = Pattern.compile(
            "@CollectLog\\([^\\n]*bizNo\\s*=\\s*\"#[^\"]*(phone|mobile|password|token|secret|authorization|openid|sessionkey|username|account|scenekey)[^\"]*\"",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> FORBIDDEN_TEXT = List.of("@Scheduled", "companyAttendanceRule");
    private static final List<String> REMOVED_FEATURE_MARKERS = List.of(
            "module." + "a" + "i",
            "A" + "i" + "Advice",
            "A" + "i" + "Analysis",
            "Dashboard" + "A" + "i",
            "a" + "i_advice",
            "A" + "I_ADVICE",
            "dashboard:" + "a" + "i",
            "a" + "iAdvice",
            "advanced" + "A" + "i",
            "A" + "I " + "\u5efa\u8bae",
            "A" + "I " + "\u4e2a\u6027\u5316",
            "behavior" + "_event",
            "behavior" + "-events",
            "Behavior" + "Event",
            "\u7ecf\u8425\u5efa\u8bae",
            "\u667a\u80fd\u5efa\u8bae"
    );

    @Test
    void sourceShouldNotReintroduceLegacySchedulerOrCacheKey() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String forbidden : FORBIDDEN_TEXT) {
                if (content.contains(forbidden)) {
                    violations.add(file + " contains " + forbidden);
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void sourceAndResourcesShouldNotReintroduceRemovedCommercialFeature() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : sourceAndResourceFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String marker : REMOVED_FEATURE_MARKERS) {
                if (content.contains(marker)) {
                    violations.add(file + " contains removed feature marker: " + marker);
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void mybatisInlineSqlShouldAvoidSelectStar() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (SELECT_STAR.matcher(content).find()) {
                violations.add(file.toString());
            }
        }
        assertTrue(violations.isEmpty(), "Use explicit columns instead of SELECT *:\n" + String.join(System.lineSeparator(), violations));
    }

    @Test
    void criticalWriteEndpointsShouldHaveOperationAudit() throws IOException {
        Map<String, List<String>> criticalMappings = Map.ofEntries(
                Map.entry("my/hive/api/auth/AdminAuthController.java", List.of("/login", "/password-reset", "/initial-password", "/scan-login/confirm")),
                Map.entry("my/hive/api/approval/ApprovalController.java", List.of("/leave/audit", "/finance/audit", "/finance", "/resignation", "/resignation/audit")),
                Map.entry("my/hive/api/employee/EmployeeController.java", List.of("/create", "/update", "/change-status", "/batch-update", "/import")),
                Map.entry("my/hive/api/order/OrderController.java", List.of("", "/{orderId}/save", "/{orderId}/status")),
                Map.entry("my/hive/api/role/RoleController.java", List.of("/create", "/role/update")),
                Map.entry("my/hive/api/print/ReceiptPrintController.java", List.of("/print/update", "/print/mark-printed", "/print/cancel", "/template/save", "/template/{id}/default")),
                Map.entry("my/hive/api/tenant/TenantManageController.java", List.of("/{id}/profile", "/{id}/logo", "/{id}/license", "/{id}/status", "/{id}/owner-account"))
        );
        assertCriticalMappingsAudited(criticalMappings);
    }

    @Test
    void tenantManualCustomWriteShouldRequireDocumentEditPermission() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/api/manual/TenantManualController.java");
        String annotationBlock = annotationBlockForMapping(file, "@PostMapping(\"/custom\")");
        assertTrue(annotationBlock.contains("@RequirePermission(value = PermissionCatalogV3.CODE_DOCUMENT_RENAME"),
                "Custom tenant manual save must require document edit permission: " + file);
    }

    @Test
    void orderControllerShouldExposeOneCanonicalOrderApi() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/api/order/OrderController.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        for (String mapping : List.of(
                "@GetMapping",
                "@GetMapping(\"/status-summary\")",
                "@GetMapping(\"/{orderId}\")",
                "@PostMapping",
                "@PostMapping(\"/{orderId}/save\")",
                "@PostMapping(\"/{orderId}/status\")",
                "@PostMapping(\"/{orderId}/advance\")",
                "@PostMapping(\"/{orderId}/rollback\")",
                "@PostMapping(\"/flow-print-task\")")) {
            assertTrue(content.contains(mapping), "Canonical order controller is missing mapping " + mapping);
        }
        assertFalse(content.contains("\"/sales/"), "Order controller must not expose retired sales-order routes");
        assertFalse(content.contains("\"/production/"), "Order controller must not expose retired production-order routes");
    }

    @Test
    void canonicalOrderResponseShouldIncludeAggregatedFulfillmentProgress() throws IOException {
        for (String voFile : List.of(
                "my/hive/domain/order/model/vo/SalesOrderPageVO.java",
                "my/hive/domain/order/model/vo/SalesOrderDetailVO.java")) {
            Path file = MAIN_SOURCE.resolve(voFile);
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String field : List.of(
                    "fulfillmentTracked",
                    "fulfillmentRecordCount",
                    "processText",
                    "currentProcessText",
                    "completedProcessText",
                    "processProgressPercent",
                    "processSteps")) {
                assertTrue(content.contains(field), "Canonical order response is missing " + field + ": " + file);
            }
        }

        Path service = MAIN_SOURCE.resolve("my/hive/domain/order/service/OrderService.java");
        String serviceContent = Files.readString(service, StandardCharsets.UTF_8);
        assertTrue(serviceContent.contains("buildFulfillmentMap(orders)"),
                "Order page must batch-load fulfillment records instead of issuing one query per row: " + service);
        assertTrue(serviceContent.contains(".in(ProductionOrder::getSalesOrderId, orderIds)"),
                "Fulfillment records must be loaded by canonical order ids: " + service);
        assertTrue(serviceContent.contains("applyFulfillmentView(vo"),
                "Order page and detail responses must apply the aggregated fulfillment view: " + service);
    }

    @Test
    void unifiedOrderMetricsShouldNotCountFulfillmentRowsAsOrders() throws IOException {
        Path warningSummary = MAIN_SOURCE.resolve("my/hive/domain/order/model/vo/OrderWarningSummaryVO.java");
        String warningSummaryContent = Files.readString(warningSummary, StandardCharsets.UTF_8);
        assertTrue(warningSummaryContent.contains("orderCount"),
                "Unified order warning summary must expose one canonical order count");
        assertFalse(warningSummaryContent.contains("salesCount"),
                "Unified order warning summary must not expose a sales-order count");
        assertFalse(warningSummaryContent.contains("productionCount"),
                "Fulfillment rows must not be exposed as a second order count");

        Path warningCache = MAIN_SOURCE.resolve("my/hive/domain/order/service/OrderWarningCacheService.java");
        String warningCacheContent = Files.readString(warningCache, StandardCharsets.UTF_8);
        assertFalse(warningCacheContent.contains("ProductionOrderMapper"),
                "Order warnings must count canonical orders only");
        assertFalse(warningCacheContent.contains("countProduction"),
                "Order warnings must not count fulfillment rows as separate orders");

        Path dashboard = MAIN_SOURCE.resolve("my/hive/domain/dashboard/service/DashboardService.java");
        String dashboardContent = Files.readString(dashboard, StandardCharsets.UTF_8);
        assertFalse(dashboardContent.contains("countMonthProductionOrders"),
                "Dashboard month order count must not add fulfillment row count");

        Path notification = MAIN_SOURCE.resolve("my/hive/domain/notification/service/NotificationService.java");
        String notificationContent = Files.readString(notification, StandardCharsets.UTF_8);
        assertFalse(notificationContent.contains("其中销售订单"),
                "Order warning notifications must use the unified order concept");
        assertFalse(notificationContent.contains("生产订单 "),
                "Order warning notifications must not expose fulfillment rows as orders");
    }

    @Test
    void managementOrderDataShouldRequireStatusPermission() throws IOException {
        Path service = MAIN_SOURCE.resolve("my/hive/domain/order/service/OrderService.java");
        String content = Files.readString(service, StandardCharsets.UTF_8);
        int methodIndex = content.indexOf("private boolean hasOrderStatusAccess");
        int methodEnd = content.indexOf("\n    }", methodIndex);
        assertTrue(methodIndex >= 0 && methodEnd > methodIndex, "OrderService must keep centralized status permission checks");
        String methodBody = content.substring(methodIndex, methodEnd);
        assertFalse(methodBody.contains("CODE_ORDER_LIST"),
                "order:list grants page entry but must not grant access to every order status");
        assertFalse(content.contains("LEGACY_ORDER_STATUS_PERMISSIONS"),
                "Retired order permission families must not bypass status permissions");

        Path dashboardMapper = MAIN_SOURCE.resolve("my/hive/domain/dashboard/mapper/DashboardMapper.java");
        String mapperContent = Files.readString(dashboardMapper, StandardCharsets.UTF_8);
        assertTrue(mapperContent.contains("<foreach collection='statuses'"),
                "Dashboard order counters must filter by the current user's permitted statuses");
    }

    @Test
    void unifiedOrderPrintingShouldNotCreateFulfillmentPrintTasks() throws IOException {
        Path service = MAIN_SOURCE.resolve("my/hive/domain/order/service/OrderService.java");
        String content = Files.readString(service, StandardCharsets.UTF_8);
        assertFalse(content.contains("createProductionOrderFlowPrintTask"),
                "Unified order printing must not expose a production-order print entry point");
        assertFalse(content.contains("buildProductionOrderFlowPrintPayload"),
                "Unified order printing must not generate fulfillment QR payloads");
        assertFalse(content.contains("\"production_order\""),
                "Unified order printing must only enqueue canonical sales-order tasks");
    }

    @Test
    void notificationAnnouncementAndSyncEndpointsShouldRequireExplicitPermission() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/api/notification/NotificationController.java");

        String announcementsBlock = annotationBlockForMapping(file, "@GetMapping(\"/announcements\")");
        assertTrue(announcementsBlock.contains("@RequirePermission(value = PermissionCatalogV3.CODE_NOTIFICATION_ANNOUNCEMENT_LIST"),
                "Enterprise announcement list must require notification announcement list permission: " + file);

        String syncBlock = annotationBlockForMapping(file, "@PostMapping(\"/sync\")");
        assertTrue(syncBlock.contains("@RequirePermission(value = PermissionCatalogV3.CODE_NOTIFICATION_ANNOUNCEMENT_PUBLISH"),
                "Manual notification sync mutates tenant-wide notification records and must require an explicit admin permission: " + file);
    }

    @Test
    void smsSenderLogsShouldNotExposeRawPhoneOrMessageContent() throws IOException {
        List<Path> smsSenders = List.of(
                MAIN_SOURCE.resolve("my/hive/infrastructure/sms/ConsoleSmsSender.java"),
                MAIN_SOURCE.resolve("my/hive/infrastructure/sms/AliyunSmsSender.java")
        );
        for (Path file : smsSenders) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            assertFalse(RAW_SMS_PHONE_LOG.matcher(content).find(),
                    "SMS logs must mask phone numbers before writing logs: " + file);
            assertFalse(RAW_SMS_CONTENT_LOG.matcher(content).find(),
                    "SMS logs must not write raw message content: " + file);
        }
    }

    @Test
    void operationLogBizNoShouldNotUseSensitiveFields() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (SENSITIVE_OPERATION_BIZ_NO.matcher(content).find()) {
                violations.add(file.toString());
            }
        }
        assertTrue(violations.isEmpty(), "Operation log bizNo is stored separately from sanitized args; do not use sensitive fields:\n"
                + String.join(System.lineSeparator(), violations));
    }

    @Test
    void authOperationLogsShouldNotRecordCredentialPayloads() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/api/auth/AdminAuthController.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<String> sensitiveActions = List.of(
                "action = \"login\"",
                "action = \"password_reset_code\"",
                "action = \"password_reset\"",
                "action = \"initial_password_change\"",
                "action = \"scan_login_confirm\""
        );
        List<String> violations = sensitiveActions.stream()
                .filter(action -> {
                    int actionIndex = content.indexOf(action);
                    if (actionIndex < 0) {
                        return true;
                    }
                    int annotationStart = content.lastIndexOf("@CollectLog", actionIndex);
                    int annotationEnd = content.indexOf(")", actionIndex);
                    if (annotationStart < 0 || annotationEnd < 0) {
                        return true;
                    }
                    String annotation = content.substring(annotationStart, annotationEnd);
                    return !annotation.contains("recordArgs = false");
                })
                .toList();
        assertTrue(violations.isEmpty(), "Auth operation logs must not record credential or login-code request payloads: " + violations);
    }

    @Test
    void orderAttachmentFilenameValidationShouldRejectPathSeparators() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/domain/order/service/OrderService.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("originalFilename.contains(\"..\")")
                        && content.contains("originalFilename.contains(\"/\")")
                        && content.contains("originalFilename.contains(\"\\\\\")"),
                "Order attachment upload must reject traversal and path separators: " + file);
    }

    @Test
    void businessAttachmentUploadsShouldRejectZeroByteFiles() throws IOException {
        Map<String, String> uploadServices = Map.of(
                "my/hive/infrastructure/storage/BusinessAttachmentService.java", "Business attachment upload",
                "my/hive/domain/order/service/OrderService.java", "Order attachment upload"
        );
        for (Map.Entry<String, String> entry : uploadServices.entrySet()) {
            Path file = MAIN_SOURCE.resolve(entry.getKey());
            String content = Files.readString(file, StandardCharsets.UTF_8);
            assertTrue(content.contains("file.getSize() <= 0"),
                    entry.getValue() + " must explicitly reject zero-byte files before storing: " + file);
        }
    }

    @Test
    void installationTaskAttachmentUrlShouldStayInsideTenantUploadDirectory() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/domain/installation/service/InstallationTaskService.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("InternalUploadUrlValidator.normalizeStoredUploadUrl"),
                "Installation task attachment URL stored from status updates must be normalized by InternalUploadUrlValidator: " + file);
        assertTrue(content.contains("tenantCode") && content.contains("\"installation-task\""),
                "Installation task attachment URL validation must bind to current tenant and installation-task module: " + file);
        assertTrue(content.contains("attachmentSize < 0"),
                "Installation task attachment size from clients must reject negative values: " + file);
    }

    @Test
    void installationTaskEndpointsShouldUseDedicatedPermissions() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/api/installation/InstallationTaskController.java");
        Map<String, String> expectedPermissions = Map.of(
                "@GetMapping(\"/page\")", "CODE_INSTALLATION_LIST",
                "@PostMapping(\"/status\")", "CODE_INSTALLATION_UPDATE",
                "@PostMapping(\"/attachment/upload\")", "CODE_INSTALLATION_ATTACHMENT_UPLOAD",
                "@GetMapping(\"/attachment/download\")", "CODE_INSTALLATION_ATTACHMENT_DOWNLOAD"
        );
        for (Map.Entry<String, String> entry : expectedPermissions.entrySet()) {
            String block = annotationBlockForMapping(file, entry.getKey());
            assertTrue(block.contains("PermissionCatalogV3." + entry.getValue()),
                    entry.getKey() + " must require " + entry.getValue());
            assertFalse(block.contains("PermissionCatalogV3.CODE_ORDER_LIST"),
                    entry.getKey() + " must not borrow order:list");
        }
    }

    @Test
    void permissionCachesShouldUseVersionedKeys() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/shared/utils/PermissionCacheUtil.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"perm-v3\""),
                "Permission reads must use the current versioned cache namespace");
        assertTrue(content.contains("\"account-v3\""),
                "Cross-app session invalidation must target the shared account-state key");
        assertFalse(content.contains("perm-v2"),
                "Retired cache namespaces must not remain in production code");
        assertFalse(content.contains("cache(\"management\", \"perm\""),
                "Management permission reads must not reuse stale pre-migration cache entries");
        assertFalse(content.contains("cache(\"mini\", \"perm\""),
                "Cross-app eviction must target the versioned mini permission key");
    }

    @Test
    void tenantRoleProvisioningShouldBeCentralized() throws IOException {
        Path tenantService = MAIN_SOURCE.resolve("my/hive/domain/tenant/service/TenantManageService.java");
        Path authService = MAIN_SOURCE.resolve("my/hive/domain/auth/service/AuthenticationService.java");
        String tenantContent = Files.readString(tenantService, StandardCharsets.UTF_8);
        String authContent = Files.readString(authService, StandardCharsets.UTF_8);

        assertTrue(tenantContent.contains("builtInRoleProvisionService.ensureTenantRoles(tenant.getTenantCode())"),
                "Owner initialization must provision the complete built-in role catalog");
        assertTrue(authContent.contains("builtInRoleProvisionService.ensureTenantRoles(tenantCode)"),
                "Organization join must provision missing built-in roles before assigning EMPLOYEE");
        assertFalse(tenantContent.contains("EMPLOYEE_BASELINE_PERMISSIONS"),
                "Employee baseline must have one source in BuiltInRoleCatalog");
    }

    @Test
    void documentNameUniquenessShouldBeScopedByTenant() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/domain/document/service/DocumentService.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        int methodIndex = content.indexOf("private void ensureNameNotExists");
        assertTrue(methodIndex >= 0, "DocumentService must keep central name uniqueness validation: " + file);
        String methodBody = content.substring(methodIndex, Math.min(content.length(), methodIndex + 900));
        assertTrue(methodBody.contains("queryWrapper.eq(Document::getTenantCode, tenantCode)"),
                "Document name uniqueness must include tenantCode to avoid cross-tenant blocking: " + file);
    }

    @Test
    void documentListReadsShouldBeScopedByTenant() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/domain/document/service/DocumentService.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        int methodIndex = content.indexOf("public List<Document> selectDocumentByParentId");
        assertTrue(methodIndex >= 0, "DocumentService must keep central document list method: " + file);
        String methodBody = content.substring(methodIndex, Math.min(content.length(), methodIndex + 700));
        assertTrue(methodBody.contains("queryWrapper.eq(Document::getTenantCode, tenantCode)"),
                "Document list reads must include tenantCode to avoid cross-tenant document leakage: " + file);
    }

    @Test
    void tenantFieldConfigReadsShouldBeScopedByTenant() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/domain/tenant/service/TenantFieldConfigService.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        int methodIndex = content.indexOf("private List<TenantFieldConfigVO> listByTenant");
        assertTrue(methodIndex >= 0, "Tenant field configuration must keep a central tenant-scoped read method: " + file);
        String methodBody = content.substring(methodIndex, Math.min(content.length(), methodIndex + 700));
        assertTrue(methodBody.contains(".eq(TenantFieldConfig::getTenantCode, tenantCode)"),
                "Tenant field configuration reads must include tenantCode to avoid cross-tenant field leakage: " + file);
    }

    @Test
    void customerSearchSubqueriesShouldBeScopedByTenant() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/domain/customer/service/CustomerService.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("FROM customer_project WHERE tenant_code = {1}"),
                "Customer project keyword subqueries must explicitly filter tenantCode: " + file);
        assertTrue(content.contains("FROM customer_contact WHERE tenant_code = {1}"),
                "Customer contact keyword subqueries must explicitly filter tenantCode: " + file);
    }

    @Test
    void customerCrudShouldKeepExplicitTenantBoundary() throws IOException {
        Path file = MAIN_SOURCE.resolve("my/hive/domain/customer/service/CustomerService.java");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains(".eq(Customer::getTenantCode, tenantCode)"),
                "Customer parent queries must explicitly filter tenantCode: " + file);
        assertTrue(content.contains(".eq(CustomerContact::getTenantCode, tenantCode)"),
                "Customer contact queries/deletes must explicitly filter tenantCode: " + file);
        assertTrue(content.contains(".eq(CustomerProject::getTenantCode, tenantCode)"),
                "Customer project queries/deletes must explicitly filter tenantCode: " + file);
    }

    @Test
    void passwordHashUpgradeShouldStayScopedByTenant() throws IOException {
        Path mapper = MAIN_SOURCE.resolve("my/hive/domain/auth/mapper/AuthMapper.java");
        String mapperContent = Files.readString(mapper, StandardCharsets.UTF_8);
        assertFalse(mapperContent.contains("@Update(\"UPDATE user SET password = #{password} WHERE id = #{userId}\")"),
                "Password hash upgrades must not write user rows without tenantCode: " + mapper);
        assertTrue(mapperContent.contains("updatePasswordHashByUserIdAndTenantCode"),
                "AuthMapper must expose a tenant-scoped password hash upgrade method: " + mapper);

        Path service = MAIN_SOURCE.resolve("my/hive/domain/auth/service/AuthenticationService.java");
        String serviceContent = Files.readString(service, StandardCharsets.UTF_8);
        assertTrue(serviceContent.contains("authMapper.updatePasswordHashByUserIdAndTenantCode("),
                "Login password hash upgrade must call the tenant-scoped method: " + service);
    }

    @Test
    void userRoleNameQueriesShouldJoinRolesWithinTenant() throws IOException {
        Path mapper = MAIN_SOURCE.resolve("my/hive/domain/permission/mapper/SysUserRoleMapper.java");
        String content = Files.readString(mapper, StandardCharsets.UTF_8);
        assertFalse(content.contains("\"INNER JOIN sys_role r ON ur.role_id = r.id \","),
                "Role name joins must not join sys_role by role_id alone: " + mapper);
        assertTrue(content.contains("INNER JOIN sys_role r ON ur.role_id = r.id AND r.tenant_code = ur.tenant_code"),
                "Role name joins must bind sys_role to the same tenant as sys_user_role: " + mapper);
    }

    @Test
    void platformTenantManagementShouldStayBehindAuthenticatedPlatformScope() throws IOException {
        Path controller = MAIN_SOURCE.resolve("my/hive/api/tenant/TenantManageController.java");
        String controllerContent = Files.readString(controller, StandardCharsets.UTF_8);
        assertTrue(controllerContent.contains("@RequestMapping(\"/platform/tenants\")"),
                "Tenant management endpoints must stay under the platform path: " + controller);

        Path webMvcConfig = MAIN_SOURCE.resolve("my/hive/shared/config/WebMvcConfig.java");
        String webMvcContent = Files.readString(webMvcConfig, StandardCharsets.UTF_8);
        int authIndex = webMvcContent.indexOf("registry.addInterceptor(tenantContextFilter)");
        int platformIndex = webMvcContent.indexOf("registry.addInterceptor(platformScopeInterceptor)");
        assertTrue(authIndex >= 0, "TenantContextFilter must be registered for management APIs: " + webMvcConfig);
        assertTrue(platformIndex > authIndex, "PlatformScopeInterceptor must run after authentication context is initialized: " + webMvcConfig);
        String interceptorBlock = webMvcContent.substring(authIndex, Math.min(webMvcContent.length(), platformIndex + 160));
        assertTrue(interceptorBlock.contains(".addPathPatterns(\"/**\")") && interceptorBlock.contains(".excludePathPatterns(PUBLIC_PATHS)"),
                "Authentication and platform scope interceptors must both protect all non-public paths: " + webMvcConfig);

        Path platformInterceptor = MAIN_SOURCE.resolve("my/hive/shared/interceptor/PlatformScopeInterceptor.java");
        String platformContent = Files.readString(platformInterceptor, StandardCharsets.UTF_8);
        assertTrue(platformContent.contains("PLATFORM_TENANT_CODE = \"super\"")
                        && platformContent.contains("PLATFORM_PATH_PREFIX = \"/platform/\"")
                        && platformContent.contains("if (path.startsWith(PLATFORM_PATH_PREFIX))")
                        && platformContent.contains("HttpStatus.FORBIDDEN"),
                "Platform tenant management must reject non-platform tenants with a 403 response: " + platformInterceptor);
    }

    @Test
    void platformTenantOwnerBootstrapShouldScopeOrganizationSeedsByTenant() throws IOException {
        Path service = MAIN_SOURCE.resolve("my/hive/domain/tenant/service/TenantManageService.java");
        String content = Files.readString(service, StandardCharsets.UTF_8);

        int departmentIndex = content.indexOf("private Department getOrCreateOwnerDepartment");
        assertTrue(departmentIndex >= 0, "Tenant owner bootstrap must keep central department seed method: " + service);
        String departmentBody = content.substring(departmentIndex, Math.min(content.length(), departmentIndex + 800));
        assertTrue(departmentBody.contains(".eq(Department::getTenantCode, tenantCode)"),
                "Owner department seed lookup must include tenantCode to avoid cross-tenant department reuse: " + service);

        int positionIndex = content.indexOf("private Position getOrCreateOwnerPosition");
        assertTrue(positionIndex >= 0, "Tenant owner bootstrap must keep central position seed method: " + service);
        String positionBody = content.substring(positionIndex, Math.min(content.length(), positionIndex + 800));
        assertTrue(positionBody.contains(".eq(Position::getTenantCode, tenantCode)"),
                "Owner position seed lookup must include tenantCode to avoid cross-tenant position reuse: " + service);
    }

    @Test
    void managementListEndpointsShouldCapPaginationAndLimits() throws IOException {
        assertSourceContains("my/hive/domain/permission/service/RoleService.java",
                List.of("MAX_PAGE_SIZE = 200", "safePageSize(size)", "Math.min(pageSize, MAX_PAGE_SIZE)"));
        assertSourceContains("my/hive/domain/employee/service/EmployeeService.java",
                List.of("MAX_PAGE_SIZE = 200", "safePageSize(query.getSize())", "Math.min(pageSize, MAX_PAGE_SIZE)", "Math.min(limit, 50)"));
        assertSourceContains("my/hive/domain/customer/service/CustomerService.java",
                List.of("MAX_PAGE_SIZE = 200", "safePageSize(request.getPageSize())", "Math.min(pageSize, MAX_PAGE_SIZE)"));
        assertSourceContains("my/hive/domain/price/service/PriceService.java",
                List.of("safeSize(request.getSize())", "Math.min(size, 100)"));
        assertSourceContains("my/hive/domain/order/service/OrderService.java",
                List.of("MAX_PAGE_SIZE = 200L", "safeSize(request.getPageSize())", "Math.min(pageSize, MAX_PAGE_SIZE)"));
        assertSourceContains("my/hive/domain/quality/service/QualityService.java",
                List.of("MAX_PAGE_SIZE = 100", "safePageSize(request.getPageSize())", "Math.min(pageSize, MAX_PAGE_SIZE)"));
        assertSourceContains("my/hive/domain/equipment/service/EquipmentService.java",
                List.of("MAX_PAGE_SIZE = 200", "safePageSize(safeRequest.getPageSize())", "Math.min(pageSize, MAX_PAGE_SIZE)"));
        assertSourceContains("my/hive/domain/attendance/service/AttendanceService.java",
                List.of("MAX_PAGE_SIZE = 200L", "normalizeRequest(request, MAX_PAGE_SIZE)", "Math.min(request.getPageSize(), maxPageSize)"));
        assertSourceContains("my/hive/domain/installation/service/InstallationTaskService.java",
                List.of("Math.min(safeRequest.getSize(), 100L)"));
        assertSourceContains("my/hive/domain/approval/service/ApprovalService.java",
                List.of("MAX_APPROVAL_LIST_LIMIT = 500", "safeApprovalListLimit(limit)", "Math.min(limit, MAX_APPROVAL_LIST_LIMIT)",
                        "MAX_AUDITOR_OPTION_LIMIT = 50", "safeAuditorOptionLimit(limit)", "Math.min(limit, MAX_AUDITOR_OPTION_LIMIT)"));
        assertSourceContains("my/hive/domain/notification/service/NotificationService.java",
                List.of("Math.min(Math.max(request.getPageSize() == null ? 20 : request.getPageSize(), 1), 100)"));
        assertSourceContains("my/hive/domain/notification/service/EnterpriseAnnouncementService.java",
                List.of("Math.min(Math.max(limit == null ? 5 : limit, 1), 50)"));
    }

    @Test
    void unifiedPrintTaskEndpointsShouldBackLabelPrintPage() throws IOException {
        Path localController = MAIN_SOURCE.resolve("my/hive/api/print/PrintTaskController.java");
        assertTrue(Files.exists(localController), "Unified print task controller must exist: " + localController);
        String content = Files.readString(localController, StandardCharsets.UTF_8);
        List<String> requiredMappings = List.of(
                "@RequestMapping(\"/print-task\")",
                "@GetMapping(\"/pending\")",
                "@GetMapping(\"/recent\")",
                "@GetMapping(\"/pending-count\")",
                "@PostMapping(\"/report\")"
        );
        List<String> violations = requiredMappings.stream()
                .filter(mapping -> !content.contains(mapping))
                .toList();
        assertTrue(violations.isEmpty(), "Label print page calls must be backed by the unified backend: " + violations);
    }

    private static void assertCriticalMappingsAudited(Map<String, List<String>> criticalMappings) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : criticalMappings.entrySet()) {
            Path file = MAIN_SOURCE.resolve(entry.getKey());
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String mapping : entry.getValue()) {
                int mappingIndex = mapping.isEmpty()
                        ? content.indexOf("@PostMapping")
                        : content.indexOf("@PostMapping(\"" + mapping + "\")");
                if (mappingIndex < 0) {
                    mappingIndex = content.indexOf("@DeleteMapping(\"" + mapping + "\")");
                }
                if (mappingIndex < 0) {
                    mappingIndex = content.indexOf("@PutMapping(\"" + mapping + "\")");
                }
                if (mappingIndex < 0) {
                    mappingIndex = content.indexOf("@PatchMapping(\"" + mapping + "\")");
                }
                if (mappingIndex < 0) {
                    violations.add(file + " missing mapping " + mapping);
                    continue;
                }
                int methodIndex = content.indexOf("public ", mappingIndex);
                String annotationBlock = content.substring(mappingIndex, methodIndex < 0 ? Math.min(content.length(), mappingIndex + 500) : methodIndex);
                if (!annotationBlock.contains("@CollectLog(")) {
                    violations.add(file + " mapping " + mapping + " missing @CollectLog");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Critical write endpoints must be audited:\n" + String.join(System.lineSeparator(), violations));
    }

    private static String annotationBlockForMapping(Path file, String mappingExpression) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        int mappingIndex = content.indexOf(mappingExpression);
        assertTrue(mappingIndex >= 0, file + " missing mapping " + mappingExpression);
        int methodIndex = content.indexOf("public ", mappingIndex);
        return content.substring(mappingIndex, methodIndex < 0 ? Math.min(content.length(), mappingIndex + 500) : methodIndex);
    }

    private static void assertSourceContains(String relativePath, List<String> requiredMarkers) throws IOException {
        Path file = MAIN_SOURCE.resolve(relativePath);
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<String> missing = requiredMarkers.stream()
                .filter(marker -> !content.contains(marker))
                .toList();
        assertTrue(missing.isEmpty(), file + " missing commercial hardening markers: " + missing);
    }

    private static List<Path> javaFiles() throws IOException {
        if (!Files.exists(MAIN_SOURCE)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(MAIN_SOURCE)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static List<Path> sourceAndResourceFiles() throws IOException {
        List<Path> roots = List.of(Path.of("src", "main", "java"), Path.of("src", "main", "resources"));
        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                files.addAll(stream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.toString();
                            return name.endsWith(".java")
                                    || name.endsWith(".yaml")
                                    || name.endsWith(".yml")
                                    || name.endsWith(".sql")
                                    || name.endsWith(".md");
                        })
                        .toList());
            }
        }
        return files;
    }

}
