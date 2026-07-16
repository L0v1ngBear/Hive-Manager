package my.hive.domain.installation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import my.hive.domain.installation.mapper.InstallationTaskInstallerMapper;
import my.hive.domain.installation.mapper.InstallationTaskMapper;
import my.hive.domain.installation.model.dto.InstallationTaskInstallerRequest;
import my.hive.domain.installation.model.dto.InstallationTaskPageRequest;
import my.hive.domain.installation.model.dto.InstallationTaskStatusUpdateRequest;
import my.hive.domain.installation.model.entity.InstallationTask;
import my.hive.domain.installation.model.entity.InstallationTaskInstaller;
import my.hive.domain.installation.model.vo.InstallationTaskVO;
import my.hive.domain.installation.service.InstallationTaskService;
import my.hive.infrastructure.storage.BusinessAttachmentService;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstallationTaskInstallerServiceTest {

    private InstallationTaskMapper taskMapper;
    private InstallationTaskInstallerMapper installerMapper;
    private InstallationTaskService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "installation-task-test"),
                InstallationTask.class
        );
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "installation-task-installer-test"),
                InstallationTaskInstaller.class
        );
        TenantPermissionContext.init("TENANT_A", 1L, Set.of());
        taskMapper = mock(InstallationTaskMapper.class);
        installerMapper = mock(InstallationTaskInstallerMapper.class);
        service = new InstallationTaskService();
        ReflectionTestUtils.setField(service, "installationTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "installationTaskInstallerMapper", installerMapper);
        ReflectionTestUtils.setField(service, "businessAttachmentService", mock(BusinessAttachmentService.class));
        ReflectionTestUtils.setField(service, "contextPath", "/api");
        when(taskMapper.updateById(any(InstallationTask.class))).thenReturn(1);
        when(installerMapper.insert(any(InstallationTaskInstaller.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void unfinishedTaskSavesAndEchoesZeroInstallers() {
        stubTask(11L);

        InstallationTaskVO result = service.updateStatus(request("production_completed", List.of()));

        assertTrue(result.getInstallers().isEmpty());
        verify(taskMapper).updateById(any(InstallationTask.class));
        verify(installerMapper).delete(any(Wrapper.class));
        verify(installerMapper, never()).insert(any(InstallationTaskInstaller.class));
    }

    @Test
    void omittedInstallersPreservesExistingRows() {
        stubTask(11L);
        when(installerMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(detail(11L, "原施工员", "100", 0)));
        InstallationTaskStatusUpdateRequest request = new InstallationTaskStatusUpdateRequest();
        request.setId(11L);
        request.setStatus("production_completed");

        InstallationTaskVO result = service.updateStatus(request);

        assertEquals(List.of("原施工员"), result.getInstallers().stream().map(item -> item.getName()).toList());
        verify(installerMapper, never()).delete(any(Wrapper.class));
        verify(installerMapper, never()).insert(any(InstallationTaskInstaller.class));
    }

    @Test
    void savesOneTrimmedInstallerWithTenantAndOrderAndEchoesIt() {
        stubTask(11L);
        AtomicLong ids = new AtomicLong(100L);
        doAnswer(invocation -> {
            InstallationTaskInstaller row = invocation.getArgument(0);
            row.setId(ids.getAndIncrement());
            return 1;
        }).when(installerMapper).insert(any(InstallationTaskInstaller.class));

        InstallationTaskVO result = service.updateStatus(request(
                "completed_accepted", List.of(installer(" 张三 ", " 010-12345678 转 801 "))));

        ArgumentCaptor<InstallationTaskInstaller> captor = ArgumentCaptor.forClass(InstallationTaskInstaller.class);
        verify(installerMapper).insert(captor.capture());
        InstallationTaskInstaller saved = captor.getValue();
        assertEquals("TENANT_A", saved.getTenantCode());
        assertEquals(11L, saved.getInstallationTaskId());
        assertEquals("张三", saved.getInstallerName());
        assertEquals("010-12345678 转 801", saved.getInstallerPhone());
        assertEquals(0, saved.getSortOrder());
        assertEquals(100L, result.getInstallers().get(0).getId());
        assertEquals("张三", result.getInstallers().get(0).getName());
        assertEquals("010-12345678 转 801", result.getInstallers().get(0).getPhone());
        assertEquals(0, result.getInstallers().get(0).getSortOrder());

        InOrder order = inOrder(taskMapper, installerMapper);
        order.verify(taskMapper).updateById(any(InstallationTask.class));
        order.verify(installerMapper).delete(any(Wrapper.class));
        order.verify(installerMapper).insert(any(InstallationTaskInstaller.class));
    }

    @Test
    void savesAndEchoesMultipleInstallersInRequestOrder() {
        stubTask(11L);

        InstallationTaskVO result = service.updateStatus(request("completed_accepted", List.of(
                installer("甲", "100"), installer("乙", "100"), installer("丙", "300"))));

        ArgumentCaptor<InstallationTaskInstaller> captor = ArgumentCaptor.forClass(InstallationTaskInstaller.class);
        verify(installerMapper, times(3)).insert(captor.capture());
        assertEquals(List.of(0, 1, 2), captor.getAllValues().stream().map(InstallationTaskInstaller::getSortOrder).toList());
        assertEquals(List.of("甲", "乙", "丙"), result.getInstallers().stream().map(item -> item.getName()).toList());
    }

    @Test
    void rejectsInvalidInstallerListsBeforeUpdatingAnything() {
        stubTask(11L);

        List<List<InstallationTaskInstallerRequest>> invalidLists = List.of(
                List.of(installer("", "100")),
                List.of(installer("　", "100")),
                List.of(installer("甲", "")),
                List.of(installer("甲".repeat(51), "100")),
                List.of(installer("甲", "1".repeat(41))),
                List.of(installer("甲", "100"), installer(" 甲 ", " 100 ")),
                installers(21)
        );

        for (List<InstallationTaskInstallerRequest> installers : invalidLists) {
            assertThrows(BusinessException.class,
                    () -> service.updateStatus(request("production_completed", installers)));
        }
        verify(taskMapper, never()).updateById(any(InstallationTask.class));
        verify(installerMapper, never()).delete(any(Wrapper.class));
        verify(installerMapper, never()).insert(any(InstallationTaskInstaller.class));
    }

    @Test
    void completedAcceptedRequiresAtLeastOneInstallerButOtherStatusesAllowEmpty() {
        stubTask(11L);

        assertThrows(BusinessException.class,
                () -> service.updateStatus(request("completed_accepted", List.of())));
        service.updateStatus(request("shipped_pending_install", List.of()));

        verify(taskMapper, times(1)).updateById(any(InstallationTask.class));
    }

    @Test
    void crossTenantTaskIsNotFoundAndInstallerRowsAreUntouched() {
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.updateStatus(request("production_completed", List.of())));

        ArgumentCaptor<Wrapper<InstallationTask>> lookup = taskWrapperCaptor();
        assertTenantPredicate(lookup.getValue());
        verify(installerMapper, never()).delete(any(Wrapper.class));
        verify(installerMapper, never()).insert(any(InstallationTaskInstaller.class));
    }

    @Test
    void replacementDeleteIsTenantScoped() {
        stubTask(11L);

        service.updateStatus(request("production_completed", List.of()));

        ArgumentCaptor<Wrapper<InstallationTaskInstaller>> captor = installerWrapperCaptorForDelete();
        assertTenantPredicate(captor.getValue());
        assertTrue(captor.getValue().getSqlSegment().contains("installation_task_id"));
    }

    @Test
    void pageBatchLoadsInstallersOnceAndGroupsThemInSortOrder() {
        InstallationTask first = task(11L);
        InstallationTask second = task(12L);
        Page<InstallationTask> source = new Page<>(1, 10, 2);
        source.setRecords(List.of(first, second));
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(source);
        when(installerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                detail(11L, "甲", "100", 0),
                detail(11L, "乙", "200", 1),
                detail(12L, "丙", "300", 0)
        ));

        Page<InstallationTaskVO> result = service.page(new InstallationTaskPageRequest());

        verify(installerMapper, times(1)).selectList(any(Wrapper.class));
        assertEquals(List.of("甲", "乙"), result.getRecords().get(0).getInstallers().stream()
                .map(item -> item.getName()).toList());
        assertEquals(List.of("丙"), result.getRecords().get(1).getInstallers().stream()
                .map(item -> item.getName()).toList());
        ArgumentCaptor<Wrapper<InstallationTaskInstaller>> captor = installerWrapperCaptorForSelect();
        assertTenantPredicate(captor.getValue());
        assertTrue(captor.getValue().getSqlSegment().contains("installation_task_id"));
    }

    @Test
    void emptyPageSkipsInstallerQuery() {
        Page<InstallationTask> source = new Page<>(1, 10, 0);
        source.setRecords(List.of());
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(source);

        Page<InstallationTaskVO> result = service.page(new InstallationTaskPageRequest());

        assertTrue(result.getRecords().isEmpty());
        verify(installerMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void taskUpdateFailureStopsReplacementAndSignalsTransactionRollback() {
        stubTask(11L);
        when(taskMapper.updateById(any(InstallationTask.class))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.updateStatus(request("production_completed", List.of())));

        verify(installerMapper, never()).delete(any(Wrapper.class));
        verify(installerMapper, never()).insert(any(InstallationTaskInstaller.class));
    }

    @Test
    void installerInsertFailurePropagatesSoTaskAndReplacementCanRollback() {
        stubTask(11L);
        when(installerMapper.insert(any(InstallationTaskInstaller.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.updateStatus(request(
                "completed_accepted", List.of(installer("甲", "100")))));

        verify(taskMapper).updateById(any(InstallationTask.class));
        verify(installerMapper).delete(any(Wrapper.class));
        verify(installerMapper).insert(any(InstallationTaskInstaller.class));
    }

    private void stubTask(Long id) {
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task(id));
    }

    private InstallationTask task(Long id) {
        InstallationTask task = new InstallationTask();
        task.setId(id);
        task.setTenantCode("TENANT_A");
        task.setOrderId("ORDER-" + id);
        task.setInstallationStatus("production_completed");
        task.setExpressCompany("物流");
        task.setExpressNo("NO-1");
        return task;
    }

    private InstallationTaskStatusUpdateRequest request(String status, List<InstallationTaskInstallerRequest> installers) {
        InstallationTaskStatusUpdateRequest request = new InstallationTaskStatusUpdateRequest();
        request.setId(11L);
        request.setStatus(status);
        request.setInstallers(installers);
        return request;
    }

    private InstallationTaskInstallerRequest installer(String name, String phone) {
        InstallationTaskInstallerRequest installer = new InstallationTaskInstallerRequest();
        installer.setName(name);
        installer.setPhone(phone);
        return installer;
    }

    private List<InstallationTaskInstallerRequest> installers(int count) {
        List<InstallationTaskInstallerRequest> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(installer("人员" + index, "电话" + index));
        }
        return result;
    }

    private InstallationTaskInstaller detail(Long taskId, String name, String phone, int sortOrder) {
        InstallationTaskInstaller detail = new InstallationTaskInstaller();
        detail.setId(taskId * 10 + sortOrder);
        detail.setTenantCode("TENANT_A");
        detail.setInstallationTaskId(taskId);
        detail.setInstallerName(name);
        detail.setInstallerPhone(phone);
        detail.setSortOrder(sortOrder);
        return detail;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Wrapper<InstallationTask>> taskWrapperCaptor() {
        ArgumentCaptor<Wrapper<InstallationTask>> captor = ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(taskMapper).selectOne(captor.capture());
        return captor;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Wrapper<InstallationTaskInstaller>> installerWrapperCaptorForDelete() {
        ArgumentCaptor<Wrapper<InstallationTaskInstaller>> captor = ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(installerMapper).delete(captor.capture());
        return captor;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Wrapper<InstallationTaskInstaller>> installerWrapperCaptorForSelect() {
        ArgumentCaptor<Wrapper<InstallationTaskInstaller>> captor = ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(installerMapper).selectList(captor.capture());
        return captor;
    }

    private void assertTenantPredicate(Wrapper<?> wrapper) {
        assertTrue(wrapper.getSqlSegment().contains("tenant_code"), wrapper.getSqlSegment());
        assertTrue(((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs().containsValue("TENANT_A"));
    }
}
