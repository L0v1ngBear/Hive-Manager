package my.hive.domain.aftersales.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import my.hive.domain.aftersales.model.dto.AfterSalesRepairImageRequest;
import my.hive.domain.aftersales.model.dto.AfterSalesTicketSaveRequest;
import my.hive.domain.aftersales.model.entity.AfterSalesTicket;
import my.hive.domain.aftersales.model.vo.AfterSalesRepairImageVO;
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

class AfterSalesRepairImageContractTest {

    private AfterSalesService service;

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("TENANT_001", 1L, Set.of());
        service = new AfterSalesService();
        ReflectionTestUtils.setField(service, "contextPath", "");
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void requestAndTicketExposeBoundedRepairImageLists() throws Exception {
        Field requestImages = AfterSalesTicketSaveRequest.class.getDeclaredField("repairImages");
        assertThat(requestImages.getType()).isEqualTo(List.class);
        assertThat(requestImages.getAnnotation(Valid.class)).isNotNull();
        assertThat(requestImages.getAnnotation(Size.class).max()).isEqualTo(9);
        assertThat(AfterSalesTicket.class.getDeclaredField("attachmentUrlsJson")).isNotNull();
        assertThat(AfterSalesTicket.class.getDeclaredField("repairImages").getType()).isEqualTo(List.class);
    }

    @Test
    void normalizesCurrentTenantRepairImagesAndRetainsOrder() {
        List<AfterSalesRepairImageVO> normalized = normalize(List.of(
                image("维修前.jpg", "/uploads/after-sales-repair/TENANT_001/before.jpg", 100L),
                image("维修后.webp", "/uploads/after-sales-repair/TENANT_001/after.webp", 200L)
        ));

        assertThat(normalized).extracting(AfterSalesRepairImageVO::getFileName)
                .containsExactly("维修前.jpg", "维修后.webp");
        assertThat(normalized).extracting(AfterSalesRepairImageVO::getFileUrl)
                .containsExactly(
                        "/uploads/after-sales-repair/TENANT_001/before.jpg",
                        "/uploads/after-sales-repair/TENANT_001/after.webp"
                );
    }

    @Test
    void rejectsForeignTenantDuplicateAndOverLimitImages() {
        assertThatThrownBy(() -> normalize(List.of(
                image("越权图片.jpg", "/uploads/after-sales-repair/OTHER_TENANT/photo.jpg", 100L)
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");

        AfterSalesRepairImageRequest duplicate =
                image("维修图.jpg", "/uploads/after-sales-repair/TENANT_001/photo.jpg", 100L);
        assertThatThrownBy(() -> normalize(List.of(duplicate, duplicate)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能重复");

        List<AfterSalesRepairImageRequest> overLimit = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            overLimit.add(image(
                    "维修图-" + index + ".jpg",
                    "/uploads/after-sales-repair/TENANT_001/photo-" + index + ".jpg",
                    100L
            ));
        }
        assertThatThrownBy(() -> normalize(overLimit))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多上传9张");
    }

    @Test
    void resolvesStoredImagesAndTreatsLegacyEmptyOrMalformedJsonAsEmpty() {
        String stored = "[{\"fileName\":\"维修后.jpg\",\"fileUrl\":\"/uploads/after-sales-repair/TENANT_001/after.jpg\",\"fileSize\":200}]";
        assertThat(resolve(stored)).singleElement().satisfies(image -> {
            assertThat(image.getFileName()).isEqualTo("维修后.jpg");
            assertThat(image.getFileSize()).isEqualTo(200L);
        });
        assertThat(resolve(null)).isEmpty();
        assertThat(resolve("not-json")).isEmpty();
    }

    private AfterSalesRepairImageRequest image(String name, String url, Long size) {
        AfterSalesRepairImageRequest image = new AfterSalesRepairImageRequest();
        image.setFileName(name);
        image.setFileUrl(url);
        image.setFileSize(size);
        return image;
    }

    @SuppressWarnings("unchecked")
    private List<AfterSalesRepairImageVO> normalize(List<AfterSalesRepairImageRequest> images) {
        return ReflectionTestUtils.invokeMethod(service, "normalizeRepairImages", images);
    }

    @SuppressWarnings("unchecked")
    private List<AfterSalesRepairImageVO> resolve(String json) {
        return ReflectionTestUtils.invokeMethod(service, "resolveRepairImages", json);
    }
}
