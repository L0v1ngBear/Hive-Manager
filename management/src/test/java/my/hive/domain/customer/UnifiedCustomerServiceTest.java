package my.hive.domain.customer;

import my.hive.domain.customer.model.dto.CustomerAddRequest;
import my.hive.domain.customer.model.dto.CustomerPageRequest;
import my.hive.domain.customer.model.dto.CustomerUpdateRequest;
import my.hive.domain.customer.service.CustomerService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UnifiedCustomerServiceTest {

    @Test
    void customerDomainUsesCanonicalSourceRoot() {
        Path source = Path.of("src/main/java");

        assertTrue(Files.exists(source.resolve("my/hive/domain/customer/service/CustomerService.java")));
        assertTrue(Files.exists(source.resolve("my/hive/api/customer/CustomerController.java")));
        assertFalse(Files.exists(source.resolve("my/management/module/customer")));
        assertFalse(Files.exists(source.resolve("my/management/controller/CustomerController.java")));
    }

    @Test
    void customerServiceKeepsCustomFieldContracts() throws Exception {
        assertMethod("addCustomer", CustomerAddRequest.class);
        assertMethod("updateCustomer", CustomerUpdateRequest.class);
        assertMethod("pageSearchCustomer", CustomerPageRequest.class);
        assertMethod("getCustomer", Long.class);
        assertMethod("listCustomerOptions", String.class);
    }

    private void assertMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = CustomerService.class.getMethod(name, parameterTypes);
        assertNotNull(method);
    }
}
