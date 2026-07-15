package my.hive.domain.order.service;

import my.hive.domain.employee.mapper.EmployeeMapper;
import my.hive.domain.employee.model.entity.Employee;
import my.hive.domain.order.mapper.SalesOrderNoteMapper;
import my.hive.domain.order.mapper.SalesOrderStatusLogMapper;
import my.hive.domain.order.model.dto.SalesOrderNoteSaveRequest;
import my.hive.domain.order.model.entity.SalesOrderNote;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderNoteServiceTest {

    private SalesOrderNoteMapper noteMapper;
    private SalesOrderStatusLogMapper logMapper;
    private EmployeeMapper employeeMapper;
    private OrderNoteService service;

    @BeforeEach
    void setUp() {
        noteMapper = mock(SalesOrderNoteMapper.class);
        logMapper = mock(SalesOrderStatusLogMapper.class);
        employeeMapper = mock(EmployeeMapper.class);
        service = new OrderNoteService(noteMapper, logMapper, employeeMapper);
        TenantPermissionContext.init("TENANT_001", 9L, Set.of(
                "order:note:view", "order:note:create", "order:note:update"));

        Employee employee = new Employee();
        employee.setId(9L);
        employee.setName("测试员工");
        when(employeeMapper.selectOne(any())).thenReturn(employee);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void createsNoteWithTrustedOperatorAndWritesOperationLog() {
        SalesOrderNoteSaveRequest request = new SalesOrderNoteSaveRequest();
        request.setContent("  客户确认包装  ");

        when(noteMapper.selectList(any())).thenReturn(List.of());
        when(noteMapper.insert(any())).thenAnswer(invocation -> {
            SalesOrderNote note = invocation.getArgument(0);
            note.setId(101L);
            return 1;
        });

        service.saveNotes("TENANT_001", "SO-1", "pending_pay", List.of(request));

        ArgumentCaptor<SalesOrderNote> captor = ArgumentCaptor.forClass(SalesOrderNote.class);
        verify(noteMapper).insert(captor.capture());
        assertEquals("客户确认包装", captor.getValue().getContent());
        assertEquals(9L, captor.getValue().getCreatorUserId());
        assertEquals("测试员工", captor.getValue().getUpdaterName());
        verify(logMapper).insert(any());
    }

    @Test
    void rejectsConcurrentUpdateWithoutOverwritingNewerContent() {
        SalesOrderNote existing = existingNote(102L, 3);
        when(noteMapper.selectList(any())).thenReturn(List.of(existing));
        when(noteMapper.updateContent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        SalesOrderNoteSaveRequest request = new SalesOrderNoteSaveRequest();
        request.setId(102L);
        request.setVersion(3);
        request.setContent("修改后的备注");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveNotes("TENANT_001", "SO-1", "pending_pay", List.of(request)));

        assertEquals(409, error.getCode());
        verify(logMapper, never()).insert(any());
    }

    @Test
    void rejectsBlankNoteAndDoesNotDeleteOmittedSavedNotes() {
        SalesOrderNoteSaveRequest blank = new SalesOrderNoteSaveRequest();
        blank.setContent("   ");

        assertThrows(BusinessException.class,
                () -> service.saveNotes("TENANT_001", "SO-1", "pending_pay", List.of(blank)));

        when(noteMapper.selectList(any())).thenReturn(List.of(existingNote(103L, 1)));
        service.saveNotes("TENANT_001", "SO-1", "pending_pay", List.of());
        verify(noteMapper, never()).delete(any());
    }

    private SalesOrderNote existingNote(Long id, int version) {
        SalesOrderNote note = new SalesOrderNote();
        note.setId(id);
        note.setTenantCode("TENANT_001");
        note.setOrderId("SO-1");
        note.setContent("原备注");
        note.setVersion(version);
        note.setCreatorUserId(8L);
        note.setCreatorName("原创建人");
        note.setUpdaterUserId(8L);
        note.setUpdaterName("原修改人");
        note.setCreateTime(LocalDateTime.now().minusDays(1));
        note.setUpdateTime(LocalDateTime.now().minusHours(1));
        return note;
    }
}
