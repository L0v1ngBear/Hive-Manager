package my.hive.shared.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTenantBoundaryGuardTest {

    @Test
    void orderReadWritePathsMustBeBoundToTheCurrentTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/order/service/OrderService.java");

        assertContains(source, ".eq(SalesOrder::getTenantCode, tenantCode)",
                "sales order list queries must restrict tenant_code");
        assertContains(source, ".eq(ProductionOrder::getTenantCode, tenantCode)",
                "production order list queries must restrict tenant_code");
        assertContains(source, ".eq(SalesOrder::getTenantCode, TenantPermissionContext.getTenantCode())",
                "sales order lookup and aggregate queries must restrict tenant_code");
        assertContains(source, ".eq(ProductionOrder::getTenantCode, TenantPermissionContext.getTenantCode())",
                "production order lookup and aggregate queries must restrict tenant_code");
        assertContains(source, ".eq(SalesOrderDetail::getTenantCode, TenantPermissionContext.getTenantCode())",
                "sales order detail queries must restrict tenant_code");
        assertContains(source, ".eq(SalesOrderStatusLog::getTenantCode, TenantPermissionContext.getTenantCode())",
                "sales order log queries must restrict tenant_code");
        assertContains(source, ".eq(ProductionOrderStatusLog::getTenantCode, TenantPermissionContext.getTenantCode())",
                "production order log queries must restrict tenant_code");
        assertContains(source, "!Objects.equals(order.getTenantCode(), TenantPermissionContext.getTenantCode())",
                "entity authorization must reject a cross-tenant order before role checks");
        assertContains(source, "applySalesOrderDataScopeFilter(wrapper);",
                "warning refresh must not reset orders outside the caller's data scope");
    }

    @Test
    void approvalOrderLookupsAndListsMustBeBoundToTheCurrentTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/approval/service/ApprovalService.java");

        assertContains(source, ".eq(SalesOrder::getTenantCode, tenantCode)",
                "approval list must restrict sales orders by tenant_code");
        assertContains(source, ".eq(ProductionOrder::getTenantCode, tenantCode)",
                "approval list must restrict production orders by tenant_code");
        assertContains(source, ".eq(SalesOrder::getTenantCode, TenantPermissionContext.getTenantCode())",
                "approval sales order lookup must restrict tenant_code");
        assertContains(source, ".eq(ProductionOrder::getTenantCode, TenantPermissionContext.getTenantCode())",
                "approval production order lookup must restrict tenant_code");
        assertContains(source, ".eq(UserLeave::getTenantCode, tenantCode)",
                "leave approval summaries and mutation guards must restrict tenant_code");
        assertContains(source, ".eq(FinanceApproval::getTenantCode, tenantCode)",
                "finance approval summaries and mutation guards must restrict tenant_code");
        assertContains(source, ".eq(ResignationApproval::getTenantCode, tenantCode)",
                "resignation approval summaries and mutation guards must restrict tenant_code");
        assertContains(source, ".eq(BadProductRecord::getTenantCode, tenantCode)",
                "quality approval lists must restrict tenant_code");
        assertContains(source, ".eq(Employee::getTenantCode, TenantPermissionContext.getTenantCode())",
                "approval applicant, auditor and escalation employee lookups must restrict tenant_code");
    }

    @Test
    void approvalCandidateQueriesAndUpdatesMustBeBoundToTheirTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/approval/service/ApprovalAuditorCandidateService.java");

        assertContains(source, ".eq(ApprovalAuditorCandidate::getTenantCode, tenantCode)",
                "approval candidate reads and writes must restrict tenant_code");
    }

    @Test
    void warningSummaryMustRespectSalesSelfAndDepartmentScope() throws IOException {
        String source = source("src/main/java/my/hive/domain/order/service/OrderWarningCacheService.java");

        assertContains(source, "applyCurrentOrderDataScope(wrapper, restrictToCurrentOrderScope);",
                "warning aggregate queries must apply the caller's order data scope");
        assertContains(source, "CODE_ORDER_SCOPE_SALES_SELF",
                "warning aggregate must support the sales-self scope");
        assertContains(source, "CODE_ORDER_SCOPE_SALES_DEPARTMENT",
                "warning aggregate must support the sales-department scope");
    }

    @Test
    void employeeOrganizationAndAttendanceRelationsMustBeBoundToTheTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/employee/service/EmployeeService.java");
        String attendanceSource = source("src/main/java/my/hive/domain/attendance/service/AttendanceService.java");

        assertContains(source, ".eq(EmployeeAttendanceLocation::getTenantCode, tenantCode)",
                "employee attendance-location reads and deletes must restrict tenant_code");
        assertContains(source, ".eq(TenantAttendanceLocation::getTenantCode, tenantCode)",
                "attendance-location validation must restrict tenant_code");
        assertContains(source, ".eq(Department::getTenantCode, TenantPermissionContext.getTenantCode())",
                "department lookups must restrict tenant_code");
        assertContains(source, ".eq(Position::getTenantCode, TenantPermissionContext.getTenantCode())",
                "position lookups must restrict tenant_code");
        assertContains(source, ".eq(EmployeeExt::getTenantCode, TenantPermissionContext.getTenantCode())",
                "employee extension lookups must restrict tenant_code");
        assertContains(attendanceSource, ".eq(TenantAttendanceLocation::getTenantCode, tenantCode)",
                "attendance location saves must never query or disable another tenant's locations");
    }

    @Test
    void organizationMutationsAndCountsMustBeBoundToTheTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/organization/service/OrganizationService.java");

        assertContains(source, ".eq(Department::getTenantCode, TenantPermissionContext.getTenantCode())",
                "department hierarchy, lookup and uniqueness operations must restrict tenant_code");
        assertContains(source, ".eq(Position::getTenantCode, TenantPermissionContext.getTenantCode())",
                "position lookup, count and uniqueness operations must restrict tenant_code");
    }

    @Test
    void priceSkuAndDependentRowsMustBeBoundToTheTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/price/service/PriceService.java");

        assertContains(source, ".eq(PriceSku::getTenantCode, tenantCode)",
                "price publish and model lookup must restrict tenant_code");
        assertContains(source, ".eq(PriceCustomerOverride::getTenantCode, TenantPermissionContext.getTenantCode())",
                "price override counts and matrix deletion must restrict tenant_code");
        assertContains(source, ".eq(PriceTierPrice::getTenantCode, tenantCode)",
                "price tier deletion must restrict tenant_code");
        assertContains(source, ".eq(Customer::getTenantCode, TenantPermissionContext.getTenantCode())",
                "price customer options and overrides must restrict tenant_code");
    }

    @Test
    void inventoryReadsWritesAndBarcodeUniquenessMustBeBoundToTheTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/inventory/service/InventoryService.java");

        assertContains(source, ".eq(Cloth::getTenantCode, TenantPermissionContext.getTenantCode())",
                "inventory list, model detail and barcode lookup must restrict tenant_code");
        assertContains(source, ".eq(Cloth::getTenantCode, tenantCode)",
                "inventory detail, outbound and barcode uniqueness must restrict tenant_code");
        assertContains(source, ".eq(ClothModelSpec::getTenantCode, tenantCode)",
                "inventory model/spec creation must restrict tenant_code");
    }

    @Test
    void labelTemplatesAndReceiptPrintRowsMustBeBoundToTheTenant() throws IOException {
        String labelSource = source("src/main/java/my/hive/domain/label/service/LabelTemplateService.java");
        String receiptSource = source("src/main/java/my/hive/domain/print/receipt/service/ReceiptPrintService.java");

        assertContains(labelSource, ".eq(LabelTemplate::getTenantCode, TenantPermissionContext.getTenantCode())",
                "label template reads and disables must restrict tenant_code");
        assertContains(receiptSource, ".eq(OutboundOrder::getTenantCode, TenantPermissionContext.getTenantCode())",
                "receipt reads and status updates must restrict tenant_code");
        assertContains(receiptSource, ".eq(OutboundItem::getTenantCode, tenantCode)",
                "receipt item reads and deletes must restrict tenant_code");
    }

    @Test
    void announcementAttachmentsMustBeBoundToTheirTenantAndModule() throws IOException {
        String source = source("src/main/java/my/hive/domain/notification/service/EnterpriseAnnouncementService.java");

        assertContains(source, "normalizeOptionalModuleAttachment(",
                "announcement attachment URLs must be validated before persistence");
        assertContains(source, "tenantCode, \"announcement\"",
                "announcement attachment URLs must be restricted to the announcement module of the current tenant");
    }

    @Test
    void organizationJoinDefaultsMustBeCreatedWithinTheTargetTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/auth/service/AuthenticationService.java");

        assertContains(source, ".eq(Department::getTenantCode, tenantCode)",
                "organization-join department lookup must restrict tenant_code");
        assertContains(source, ".eq(Position::getTenantCode, tenantCode)",
                "organization-join position lookup must restrict tenant_code");
    }

    @Test
    void tenantOwnerExtensionMustBeBoundToTheTargetTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/tenant/service/TenantManageService.java");

        assertContains(source, "selectIncludingDeleted(tenantCode, userId)",
                "tenant owner extension lookup must restrict tenant_code through its scoped mapper method");
    }

    @Test
    void qualityCreatorLookupMustBeBoundToTheCurrentTenant() throws IOException {
        String source = source("src/main/java/my/hive/domain/quality/service/QualityService.java");

        assertContains(source, ".eq(Employee::getTenantCode, tenantCode)",
                "quality records must not copy a creator name from another tenant");
    }

    @Test
    void chunkedAnnouncementAttachmentsMustUseTheSameModuleContractAsTheController() throws IOException {
        String source = source("src/main/java/my/hive/infrastructure/storage/ChunkedVideoUploadService.java");

        assertContains(source, "\"announcement\"",
                "the chunked-upload module allowlist must support published announcement attachments");
    }

    @Test
    void malformedTokensMustFailClosedWithoutExposingParserErrors() throws IOException {
        String source = source("src/main/java/my/hive/shared/utils/TokenUtil.java");

        assertContains(source, "MessageDigest.isEqual(",
                "token signatures must be compared in constant time");
        assertContains(source, "catch (RuntimeException exception)",
                "malformed token payloads must be treated as invalid sessions");
    }

    private String source(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private void assertContains(String source, String expected, String message) {
        assertTrue(source.contains(expected), message + ": " + expected);
    }
}
