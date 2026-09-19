package my.hive.domain.aftersales.service;

import my.hive.domain.aftersales.mapper.*;
import my.hive.domain.aftersales.model.dto.*;
import my.hive.domain.aftersales.model.entity.*;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AfterSalesTreatmentServiceTest {
    AfterSalesTreatmentService service;
    AfterSalesTicketMapper tickets;
    AfterSalesTreatmentMapper treatments;
    AfterSalesTicket ticket;

    @BeforeEach void setup() {
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "treatment-test");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, AfterSalesPart.class);
        TenantPermissionContext.init("T1", 7L, Set.of("after_sales:process"));
        service = new AfterSalesTreatmentService();
        tickets = mock(AfterSalesTicketMapper.class);
        treatments = mock(AfterSalesTreatmentMapper.class);
        ReflectionTestUtils.setField(service, "ticketMapper", tickets);
        ReflectionTestUtils.setField(service, "treatmentMapper", treatments);
        ReflectionTestUtils.setField(service, "afterSalesService", mock(AfterSalesService.class));
        ticket = new AfterSalesTicket();
        ticket.setId(1L); ticket.setTenantCode("T1"); ticket.setTicketNo("AS1");
        ticket.setStatus("closed"); ticket.setAssigneeUserId(7L); ticket.setAssigneeName("处理人");
        ticket.setOpeningDate(LocalDate.of(2020, 2, 29)); ticket.setDiagnosis("原故障研判");
        ticket.setResolution("第一次处理结果"); ticket.setFollowUpContent("第一次回访未解决");
        when(tickets.selectOne(any())).thenReturn(ticket);
        when(treatments.selectList(any())).thenReturn(List.of());
        when(treatments.insert(any())).thenAnswer(i -> { ((AfterSalesTreatment)i.getArgument(0)).setId(2L); return 1; });
    }
    @AfterEach void cleanup() { TenantPermissionContext.clear(); }
    AfterSalesTreatmentSaveRequest request() {
        var request = new AfterSalesTreatmentSaveRequest();
        request.setRequestKey("intent-1"); request.setTreatmentType("motor_replacement");
        request.setDescription("第二次更换电机"); request.setNewMotorModel("M2"); request.setMotorQuantity(1);
        request.setParts(List.of()); request.setRepairImages(List.of());
        return request;
    }
    @Test void appendPreservesOriginalWarrantyAndHistory() {
        var round = service.create(1L, request());
        assertEquals(ticket.getOpeningDate(), round.getOriginalOpeningDate());
        assertEquals("第一次处理结果", ticket.getResolution());
        assertEquals("第一次回访未解决", ticket.getFollowUpContent());
        assertEquals("processing", ticket.getStatus());
        assertEquals("processing", round.getStatus());
        assertEquals(1, round.getSequenceNo());
    }
    @Test void missingTenantTicketCannotCreateTreatment() {
        when(tickets.selectOne(any())).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.create(1L, request()));
        verify(treatments, never()).insert(any());
    }
    @Test void otherAssigneeCannotCreateTreatment() {
        ticket.setAssigneeUserId(99L);
        assertThrows(BusinessException.class, () -> service.create(1L, request()));
        verify(treatments, never()).insert(any());
    }
    @Test void cancelledTicketCannotReopen() {
        ticket.setStatus("cancelled");
        assertThrows(BusinessException.class, () -> service.create(1L, request()));
    }
    @Test void completedRecordAcceptsOneFollowUpWithoutOverwritingOtherRounds() {
        var record = service.create(1L, request());
        when(treatments.selectOne(any())).thenReturn(record);
        var action = new AfterSalesTreatmentActionRequest(); action.setVersion(0); action.setResolution("更换完成");
        service.action(1L, 2L, "complete", action);
        assertEquals("waiting_follow_up", record.getStatus());
        assertThrows(BusinessException.class, () -> service.action(1L, 2L, "complete", action));
        action.setVersion(1);
        var follow = new AfterSalesTicketFollowUpRequest(); follow.setSatisfaction("满意"); follow.setResolved(true); follow.setContent("正常运行");
        action.setFollowUp(follow);
        service.action(1L, 2L, "follow-up", action);
        assertEquals("resolved", record.getStatus());
        assertEquals("第一次回访未解决", ticket.getFollowUpContent());
        action.setVersion(2);
        assertThrows(BusinessException.class, () -> service.action(1L, 2L, "follow-up", action));
    }
    @Test void requestRetryDoesNotCreateSecondTreatment() {
        var record = service.create(1L, request());
        when(treatments.selectList(any())).thenReturn(List.of(record));
        assertEquals(record.getId(), service.create(1L, request()).getId());
        verify(treatments, times(1)).insert(any());
        var changed = request(); changed.setDescription("不同内容");
        assertThrows(BusinessException.class, () -> service.create(1L, changed));
    }
    @Test void outboundWritesOnlyThisRoundsLedgerAndCannotRepeat() {
        TenantPermissionContext.init("T1", 7L, Set.of("after_sales:process", "after_sales:part:outbound"));
        var parts = mock(AfterSalesPartMapper.class);
        var ledger = mock(AfterSalesPartStockRecordMapper.class);
        ReflectionTestUtils.setField(service, "partMapper", parts);
        ReflectionTestUtils.setField(service, "stockRecordMapper", ledger);
        var part = new AfterSalesPart(); part.setId(10L); part.setStockQty(10); part.setVersion(0); part.setTenantCode("T1");
        when(parts.selectOne(any())).thenReturn(part); when(parts.update(isNull(), any())).thenReturn(1);
        var input = request(); input.setTreatmentType("parts_and_motor");
        var line = new AfterSalesTicketSaveRequest.AfterSalesTicketPartItem(); line.setPartId(10L); line.setQuantity(2); line.setPartLocation("一车间");
        input.setParts(List.of(line));
        var record = service.create(1L, input); when(treatments.selectOne(any())).thenReturn(record);
        var action = new AfterSalesTreatmentActionRequest(); action.setVersion(0);
        assertEquals("waiting_outbound", record.getStatus());
        service.action(1L, 2L, "outbound", action);
        assertEquals("processing", record.getStatus());
        assertEquals("outbound", record.getParts().get(0).getLineStatus());
        var capture = org.mockito.ArgumentCaptor.forClass(AfterSalesPartStockRecord.class);
        verify(ledger).insert(capture.capture());
        assertEquals(2L, capture.getValue().getTreatmentId()); assertEquals(1L, capture.getValue().getTicketId());
        assertEquals(8, capture.getValue().getAfterQty());
        action.setVersion(1);
        assertThrows(BusinessException.class, () -> service.action(1L, 2L, "outbound", action));
        verify(ledger, times(1)).insert(any());
        var editing = request(); editing.setVersion(1); editing.setTreatmentType("parts_and_motor"); editing.setParts(List.of());
        assertThrows(BusinessException.class, () -> service.update(1L, 2L, editing));
    }
    @Test void mainTicketCannotCloseWhileAnotherRoundStillNeedsFollowUp() {
        var original = new AfterSalesService();
        ReflectionTestUtils.setField(original, "ticketMapper", tickets);
        ReflectionTestUtils.setField(original, "treatmentMapper", treatments);
        ticket.setStatus("processing");
        var record = new AfterSalesTreatment(); record.setStatus("waiting_follow_up");
        when(treatments.selectList(any())).thenReturn(List.of(record));
        var action = new AfterSalesTicketStatusRequest(); action.setTicketId(1L); action.setAction("close"); action.setResolution("结案");
        assertThrows(BusinessException.class, () -> original.updateTicketStatus(action));
        verify(tickets, never()).updateById(any());
    }
    @Test void finalCaseClosurePreservesInitialResultAndAppendsEachClosure() {
        var original = new AfterSalesService();
        ReflectionTestUtils.setField(original, "ticketMapper", tickets);
        ReflectionTestUtils.setField(original, "treatmentMapper", treatments);
        ticket.setStatus("processing");
        var record = new AfterSalesTreatment(); record.setStatus("resolved");
        when(treatments.selectList(any())).thenReturn(List.of(record));
        var action = new AfterSalesTicketStatusRequest(); action.setTicketId(1L); action.setAction("close"); action.setResolution("第二次处理后结案");
        original.updateTicketStatus(action);
        assertEquals("第一次处理结果", ticket.getResolution());
        assertEquals(1, com.alibaba.fastjson2.JSON.parseArray(ticket.getTreatmentClosuresJson()).size());
        ticket.setStatus("processing"); action.setResolution("第三次处理后结案"); original.updateTicketStatus(action);
        assertEquals(2, com.alibaba.fastjson2.JSON.parseArray(ticket.getTreatmentClosuresJson()).size());
        assertTrue(ticket.getTreatmentClosuresJson().contains("第二次处理后结案"));
    }
}
