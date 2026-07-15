package my.hive.domain.notification;

import my.hive.domain.notification.mapper.NotificationMapper;
import my.hive.domain.notification.model.entity.NotificationRecord;
import my.hive.domain.notification.service.NotificationService;
import my.hive.shared.context.TenantPermissionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationTenantIsolationTest {

    @AfterEach
    void clearContext() {
        TenantPermissionContext.clear();
    }

    @Test
    void markReadUsesTenantAndUserFromTheSharedContext() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationService();
        ReflectionTestUtils.setField(service, "notificationMapper", mapper);
        TenantPermissionContext.init("TENANT_A", 7L, Set.of());
        when(mapper.selectByIdForUser("TENANT_A", 7L, 19L)).thenReturn(new NotificationRecord());

        service.markRead(19L);

        verify(mapper).markRead("TENANT_A", 7L, 19L);
    }

    @Test
    void markReadDoesNotUpdateARecordOutsideTheSharedContext() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationService();
        ReflectionTestUtils.setField(service, "notificationMapper", mapper);
        TenantPermissionContext.init("TENANT_A", 7L, Set.of());

        service.markRead(19L);

        verify(mapper, never()).markRead("TENANT_A", 7L, 19L);
    }
}
