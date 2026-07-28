package my.hive.domain.order.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import my.hive.domain.order.model.dto.SalesOrderAttachmentSaveRequest;
import my.hive.domain.order.model.dto.SalesOrderSaveRequest;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.vo.SalesOrderAttachmentVO;
import my.hive.domain.order.model.vo.SalesOrderDetailVO;
import my.hive.domain.order.model.vo.SalesOrderPageVO;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderMultiAttachmentContractTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 1L, Set.of());
        service = new OrderService();
        ReflectionTestUtils.setField(service, "contextPath", "");
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void requestAndResponsesExposeBoundedAttachmentLists() throws Exception {
        Field requestAttachments = SalesOrderSaveRequest.class.getDeclaredField("attachments");
        assertThat(requestAttachments.getType()).isEqualTo(List.class);
        assertThat(requestAttachments.getAnnotation(Valid.class)).isNotNull();
        assertThat(requestAttachments.getAnnotation(Size.class).max()).isEqualTo(20);
        assertThat(SalesOrder.class.getDeclaredField("attachmentsJson")).isNotNull();
        assertThat(SalesOrderDetailVO.class.getDeclaredField("attachments").getType()).isEqualTo(List.class);
        assertThat(SalesOrderPageVO.class.getDeclaredField("attachments").getType()).isEqualTo(List.class);
    }

    @Test
    void normalizesMultipleInternalAttachmentsAndRetainsOrder() {
        SalesOrderSaveRequest request = new SalesOrderSaveRequest();
        request.setAttachments(List.of(
                attachment("合同.pdf", "/uploads/sales-order/TENANT_001/contract.pdf", 100L),
                attachment("图纸.zip", "/uploads/sales-order/TENANT_001/drawing.zip", 200L)
        ));

        List<SalesOrderAttachmentVO> normalized = normalize(request);

        assertThat(normalized).extracting(SalesOrderAttachmentVO::getFileName)
                .containsExactly("合同.pdf", "图纸.zip");
        assertThat(normalized).extracting(SalesOrderAttachmentVO::getFileUrl)
                .containsExactly(
                        "/uploads/sales-order/TENANT_001/contract.pdf",
                        "/uploads/sales-order/TENANT_001/drawing.zip"
                );
    }

    @Test
    void fallsBackToLegacyScalarAttachmentForOldClientsAndRows() {
        SalesOrderSaveRequest request = new SalesOrderSaveRequest();
        request.setAttachmentName("旧合同.pdf");
        request.setAttachmentUrl("/uploads/sales-order/TENANT_001/legacy.pdf");
        request.setAttachmentSize(300L);

        assertThat(normalize(request)).singleElement().satisfies(attachment -> {
            assertThat(attachment.getFileName()).isEqualTo("旧合同.pdf");
            assertThat(attachment.getFileSize()).isEqualTo(300L);
        });

        SalesOrder order = new SalesOrder();
        order.setAttachmentName("历史附件.zip");
        order.setAttachmentUrl("/uploads/sales-order/TENANT_001/history.zip");
        order.setAttachmentSize(400L);
        List<SalesOrderAttachmentVO> resolved = resolve(order);
        assertThat(resolved).singleElement().extracting(SalesOrderAttachmentVO::getFileName)
                .isEqualTo("历史附件.zip");
    }

    @Test
    void rejectsDuplicateAndOverLimitAttachmentCollections() {
        SalesOrderAttachmentSaveRequest duplicate =
                attachment("合同.pdf", "/uploads/sales-order/TENANT_001/contract.pdf", 100L);
        SalesOrderSaveRequest duplicateRequest = new SalesOrderSaveRequest();
        duplicateRequest.setAttachments(List.of(duplicate, duplicate));
        assertThatThrownBy(() -> normalize(duplicateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能重复");

        SalesOrderSaveRequest overLimit = new SalesOrderSaveRequest();
        List<SalesOrderAttachmentSaveRequest> attachments = new ArrayList<>();
        for (int index = 0; index < 21; index++) {
            attachments.add(attachment(
                    "附件-" + index,
                    "/uploads/sales-order/TENANT_001/file-" + index,
                    1L
            ));
        }
        overLimit.setAttachments(attachments);
        assertThatThrownBy(() -> normalize(overLimit))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多添加20个");
    }

    private SalesOrderAttachmentSaveRequest attachment(String name, String url, Long size) {
        SalesOrderAttachmentSaveRequest attachment = new SalesOrderAttachmentSaveRequest();
        attachment.setFileName(name);
        attachment.setFileUrl(url);
        attachment.setFileSize(size);
        return attachment;
    }

    @SuppressWarnings("unchecked")
    private List<SalesOrderAttachmentVO> normalize(SalesOrderSaveRequest request) {
        return ReflectionTestUtils.invokeMethod(service, "normalizeSalesOrderAttachments", request);
    }

    @SuppressWarnings("unchecked")
    private List<SalesOrderAttachmentVO> resolve(SalesOrder order) {
        return ReflectionTestUtils.invokeMethod(service, "resolveSalesOrderAttachments", order);
    }
}
