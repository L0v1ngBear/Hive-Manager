package my.management.module.order.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.order.mapper.ProductionOrderMapper;
import my.management.module.order.model.dto.ProductionOrderSaveRequest;
import my.management.module.order.model.entity.ProductionOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionOrderInformationChannelValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private CodeGeneratorUtil codeGeneratorUtil;

    @Mock
    private ProductionOrderMapper productionOrderMapper;

    @Mock
    private OrderWarningCacheService orderWarningCacheService;

    private OrderService subject;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        TenantPermissionContext.init("tenant-a", 1L, Set.of("*"));
        subject = new OrderService();
        ReflectionTestUtils.setField(subject, "codeGeneratorUtil", codeGeneratorUtil);
        ReflectionTestUtils.setField(subject, "productionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(subject, "orderWarningCacheService", orderWarningCacheService);
    }

    @AfterEach
    void tearDown() {
        TenantPermissionContext.clear();
    }

    @Test
    void ordinaryProductionOrderRequiresInformationChannel() {
        ProductionOrderSaveRequest request = validRequest("bulk", "   ");

        Set<ConstraintViolation<ProductionOrderSaveRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
                "informationChannel".equals(violation.getPropertyPath().toString())));
    }

    @Test
    void drawingBudgetProductionOrderAllowsBlankInformationChannel() {
        ProductionOrderSaveRequest request = validRequest("drawing_budget", null);

        Set<ConstraintViolation<ProductionOrderSaveRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().noneMatch(violation ->
                "informationChannel".equals(violation.getPropertyPath().toString())));
    }

    @Test
    void serviceRejectsOrdinaryProductionOrderWithoutInformationChannel() {
        when(codeGeneratorUtil.generateProductionOrderCode()).thenReturn("PO-100");
        ProductionOrderSaveRequest request = validRequest("bulk", "  ");

        assertThrows(BusinessException.class, () -> subject.createProductionOrder(request));

        verify(productionOrderMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void servicePersistsDrawingBudgetProductionOrderWithoutInformationChannel() {
        when(codeGeneratorUtil.generateProductionOrderCode()).thenReturn("PO-100");
        ProductionOrderSaveRequest request = validRequest("drawing_budget", null);

        subject.createProductionOrder(request);

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(productionOrderMapper).insert(captor.capture());
        assertEquals("drawing_budget", captor.getValue().getOrderCategory());
        assertNull(captor.getValue().getInformationChannel());
    }

    private ProductionOrderSaveRequest validRequest(String category, String informationChannel) {
        ProductionOrderSaveRequest request = new ProductionOrderSaveRequest();
        request.setOrderCategory(category);
        request.setModelCode("MODEL-1");
        request.setWeight(1.2F);
        request.setSpec(2.8F);
        request.setQuantity(1);
        request.setInformationChannel(informationChannel);
        return request;
    }
}
