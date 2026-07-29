package my.hive.domain.order.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import my.hive.domain.order.model.dto.SalesOrderSaveRequest;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesOrderProductionLocationValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void allowsOnlyBeijingHainingOrMissingLocation() {
        assertTrue(productionLocationViolations("北京").isEmpty());
        assertTrue(productionLocationViolations("海宁分公司").isEmpty());
        assertTrue(productionLocationViolations(null).isEmpty());
        assertEquals(1, productionLocationViolations("上海").size());
    }

    @Test
    void serviceNormalizesAllowedLocationsAndRejectsOtherValues() {
        OrderService subject = new OrderService();

        assertEquals("北京", ReflectionTestUtils.invokeMethod(
                subject, "normalizeSalesProductionLocation", " 北京 "));
        assertEquals("海宁分公司", ReflectionTestUtils.invokeMethod(
                subject, "normalizeSalesProductionLocation", "海宁分公司"));
        assertNull(ReflectionTestUtils.invokeMethod(
                subject, "normalizeSalesProductionLocation", " "));
        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                subject, "normalizeSalesProductionLocation", "上海"));
    }

    private Set<ConstraintViolation<SalesOrderSaveRequest>> productionLocationViolations(String location) {
        SalesOrderSaveRequest request = new SalesOrderSaveRequest();
        request.setCustomerName("测试客户");
        request.setProjectName("测试项目");
        request.setProductionLocation(location);
        return validator.validate(request).stream()
                .filter(violation -> "productionLocation".equals(violation.getPropertyPath().toString()))
                .collect(java.util.stream.Collectors.toSet());
    }
}
