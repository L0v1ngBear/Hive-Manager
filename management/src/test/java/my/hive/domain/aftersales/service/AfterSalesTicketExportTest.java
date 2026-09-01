package my.hive.domain.aftersales.service;

import my.hive.domain.aftersales.mapper.AfterSalesTicketMapper;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketPageRequest;
import my.hive.domain.aftersales.model.entity.AfterSalesTicket;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.utils.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AfterSalesTicketExportTest {

    private final AfterSalesTicketMapper ticketMapper = mock(AfterSalesTicketMapper.class);
    private AfterSalesService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 7L, Set.of());
        service = new AfterSalesService();
        ReflectionTestUtils.setField(service, "ticketMapper", ticketMapper);
        ReflectionTestUtils.setField(service, "excelUtil", new ExcelUtil());
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void buildsCompleteReadableWorkbookBeforeReturningDownload() throws Exception {
        AfterSalesTicket ticket = new AfterSalesTicket();
        ticket.setTicketNo("AS202609010001");
        ticket.setCreateTime(LocalDateTime.of(2026, 9, 1, 10, 30));
        ticket.setCustomerName("示例客户");
        ticket.setTicketType("consultation");
        ticket.setStatus("draft");
        ticket.setPriority("normal");
        ticket.setProblemDesc("遥控器无法使用");
        when(ticketMapper.selectCount(any())).thenReturn(1L);
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        byte[] content = service.exportTickets(new AfterSalesTicketPageRequest());

        assertThat(content).isNotEmpty();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("售后工单");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("工单号");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo("AS202609010001");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue()).isEmpty();
            assertThat(workbook.getSheetAt(0).getColumnWidth(0)).isEqualTo(20 * 256);
            assertThat(workbook.getSheetAt(0).getColumnWidth(20)).isEqualTo(20 * 256);
        }
    }
}
