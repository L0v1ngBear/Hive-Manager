package my.management.controller;

import my.hive.common.annotation.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmployeePermissionProfileContractTest {

    @Test
    void exposesOnlyTheV3PermissionProfileContract() {
        Method getProfile = method("permissionProfile");
        Method updateOverrides = method("updatePermissionOverrides");

        assertEquals("/{id}/permission-profile", getProfile.getAnnotation(GetMapping.class).value()[0]);
        assertEquals("/{id}/permission-overrides", updateOverrides.getAnnotation(PutMapping.class).value()[0]);
        assertArrayEquals(new String[]{"employee:permission:manage"},
                getProfile.getAnnotation(RequirePermission.class).value());
        assertArrayEquals(new String[]{"employee:permission:manage"},
                updateOverrides.getAnnotation(RequirePermission.class).value());

        assertFalse(Arrays.stream(EmployeeController.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("permissionOverrides")));
    }

    private Method method(String name) {
        return Arrays.stream(EmployeeController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
