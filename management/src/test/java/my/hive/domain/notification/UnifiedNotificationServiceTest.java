package my.hive.domain.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedNotificationServiceTest {

    private static final Path MAIN_SOURCE = Path.of("src/main/java");

    @Test
    void notificationRuntimeUsesCanonicalDomainAndApiPackages() {
        assertThat(MAIN_SOURCE.resolve("my/hive/domain/notification/service/NotificationService.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/domain/notification/service/EnterpriseAnnouncementService.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/api/notification/NotificationController.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/management/module/notification")).doesNotExist();
        assertThat(MAIN_SOURCE.resolve("my/management/controller/NotificationController.java")).doesNotExist();
    }

    @Test
    void smsTransportIsOutsideTheNotificationDomain() {
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/sms/SmsSender.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/sms/SmsVerificationService.java")).exists();
    }

    @Test
    void wechatSubscriptionUsesOneCanonicalAdapterAndPersistenceModel() {
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/wechat/WechatSubscribeService.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/infrastructure/wechat/WechatSubscribeUser.java")).exists();
        assertThat(MAIN_SOURCE.resolve("my/hive/api/wechat/WechatSubscribeController.java")).exists();
    }
}
