package my.hive.domain.quality;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import my.hive.domain.approval.service.ApprovalAuditorCandidateService;
import my.hive.domain.approval.service.ApprovalDefaultAuditorService;
import my.hive.domain.quality.mapper.BadProductMapper;
import my.hive.domain.quality.model.dto.BadProductPageRequest;
import my.hive.domain.quality.model.dto.BadProductProcessRequest;
import my.hive.domain.quality.model.dto.BadProductSaveRequest;
import my.hive.domain.quality.model.entity.BadProductRecord;
import my.hive.domain.quality.service.QualityService;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.permission.PermissionCatalogV3;
import my.management.module.employee.mapper.EmployeeMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyBoolean;

@SuppressWarnings({"unchecked", "rawtypes"})
class UnifiedQualityServiceTest {

    @BeforeAll
    static void initializeMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), BadProductRecord.class);
    }

    @AfterEach
    void clearTenantContext() {
        TenantPermissionContext.clear();
    }

    @Test
    void canonicalQualitySourcesReplaceLegacyBadProductSources() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/quality/service/QualityService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/quality/QualityController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/badproduct/service/BadProductService.java")));
        assertFalse(Files.exists(source.resolve("my/management/controller/BadProductController.java")));
    }

    @Test
    void qualityRecordAndApprovalFlowUseOneTransactionalService() throws Exception {
        assertRollbackTransaction("save", BadProductSaveRequest.class);
        assertRollbackTransaction("process", BadProductProcessRequest.class);
        assertRollbackTransaction("approveProcess", String.class);
        assertRollbackTransaction("rejectProcessApproval", String.class);
    }

    @Test
    void attachmentLifecycleNormalizesNamesAndBlankValues() {
        QualityService service = new QualityService();

        assertEquals("quality-attachment",
                ReflectionTestUtils.invokeMethod(service, "normalizeAttachmentName", null, "/api/uploads/bad-product/TENANT/a.png"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "normalizeAttachmentSize", 100L, null));
    }

    @Test
    void duplicateQualitySubmissionGuardRemainsExplicit() throws Exception {
        Method method = QualityService.class.getMethod("hasPendingQualityApproval", String.class);

        assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    void pageQueryIsTenantScoped() {
        TenantPermissionContext.init("tenant-a", 7L, Set.of(PermissionCatalogV3.CODE_QUALITY_LIST));
        BadProductMapper mapper = mock(BadProductMapper.class);
        when(mapper.selectPage(any(), any())).thenReturn(new Page<>());
        QualityService service = serviceWithMapper(mapper);

        service.page(new BadProductPageRequest());

        ArgumentCaptor<Wrapper<BadProductRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectPage(any(), captor.capture());
        assertTenantScoped(captor.getValue(), "tenant-a");
    }

    @Test
    void saveUpdateLookupIsTenantScopedAndRequiresUpdatePermission() {
        TenantPermissionContext.init("tenant-a", 7L, Set.of(PermissionCatalogV3.CODE_QUALITY_UPDATE));
        BadProductMapper mapper = mock(BadProductMapper.class);
        BadProductRecord existing = qualityRecord("tenant-a", "Q-1", "pending");
        when(mapper.selectOne(any())).thenReturn(existing);
        QualityService service = serviceWithMapper(mapper);

        BadProductSaveRequest request = new BadProductSaveRequest();
        request.setDefectiveId("Q-1");
        request.setType("fabric");

        service.save(request);

        ArgumentCaptor<Wrapper<BadProductRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(captor.capture());
        assertTenantScoped(captor.getValue(), "tenant-a");
    }

    @Test
    void processLookupIsTenantScoped() {
        TenantPermissionContext.init("tenant-a", 7L, Set.of(PermissionCatalogV3.CODE_QUALITY_PROCESS));
        BadProductMapper mapper = mock(BadProductMapper.class);
        BadProductRecord existing = qualityRecord("tenant-a", "Q-1", "pending");
        when(mapper.selectOne(any())).thenReturn(existing).thenReturn(null);
        ApprovalDefaultAuditorService defaultAuditors = mock(ApprovalDefaultAuditorService.class);
        when(defaultAuditors.resolveAuditorIds(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(9L));
        ApprovalAuditorCandidateService candidates = mock(ApprovalAuditorCandidateService.class);
        QualityService service = serviceWithMapper(mapper);
        ReflectionTestUtils.setField(service, "approvalDefaultAuditorService", defaultAuditors);
        ReflectionTestUtils.setField(service, "approvalAuditorCandidateService", candidates);

        BadProductProcessRequest request = new BadProductProcessRequest();
        request.setDefectiveId("Q-1");
        request.setMethod("repair");

        service.process(request);

        ArgumentCaptor<Wrapper<BadProductRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper, org.mockito.Mockito.times(2)).selectOne(captor.capture());
        assertTenantScoped(captor.getAllValues().get(0), "tenant-a");
        assertTenantScoped(captor.getAllValues().get(1), "tenant-a");
    }

    @Test
    void approvalCompletionLookupsAreTenantScoped() {
        TenantPermissionContext.init("tenant-a", 7L, Set.of(PermissionCatalogV3.CODE_QUALITY_AUDIT));
        BadProductMapper mapper = mock(BadProductMapper.class);
        when(mapper.selectOne(any()))
                .thenReturn(qualityRecord("tenant-a", "Q-1", "pending_audit"))
                .thenReturn(qualityRecord("tenant-a", "Q-2", "pending_audit"));
        QualityService service = serviceWithMapper(mapper);

        service.approveProcess("Q-1");
        service.rejectProcessApproval("Q-2");

        ArgumentCaptor<Wrapper<BadProductRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper, org.mockito.Mockito.times(2)).selectOne(captor.capture());
        assertTenantScoped(captor.getAllValues().get(0), "tenant-a");
        assertTenantScoped(captor.getAllValues().get(1), "tenant-a");
    }

    private void assertRollbackTransaction(String name, Class<?>... parameterTypes) throws Exception {
        Method method = QualityService.class.getMethod(name, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, name + " must be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

    private QualityService serviceWithMapper(BadProductMapper mapper) {
        QualityService service = new QualityService();
        ReflectionTestUtils.setField(service, "badProductMapper", mapper);
        ReflectionTestUtils.setField(service, "employeeMapper", mock(EmployeeMapper.class));
        return service;
    }

    private BadProductRecord qualityRecord(String tenantCode, String defectiveId, String status) {
        BadProductRecord record = new BadProductRecord();
        record.setId(1L);
        record.setTenantCode(tenantCode);
        record.setDefectiveId(defectiveId);
        record.setStatus(status);
        return record;
    }

    private void assertTenantScoped(Wrapper<BadProductRecord> wrapper, String tenantCode) {
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_code") || sqlSegment.contains("tenantCode"), sqlSegment);
        assertTrue(wrapper instanceof AbstractWrapper<?, ?, ?>, "Expected MyBatis-Plus AbstractWrapper");
        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        assertTrue(abstractWrapper.getParamNameValuePairs().values().stream().map(String::valueOf).anyMatch(tenantCode::equals),
                "Expected tenant code in wrapper parameters: " + abstractWrapper.getParamNameValuePairs());
    }
}
