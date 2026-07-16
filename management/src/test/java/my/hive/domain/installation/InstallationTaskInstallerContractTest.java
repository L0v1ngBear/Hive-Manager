package my.hive.domain.installation;

import com.baomidou.mybatisplus.annotation.TableName;
import my.hive.domain.installation.model.dto.InstallationTaskInstallerRequest;
import my.hive.domain.installation.model.dto.InstallationTaskStatusUpdateRequest;
import my.hive.domain.installation.model.entity.InstallationTask;
import my.hive.domain.installation.model.entity.InstallationTaskInstaller;
import my.hive.domain.installation.model.vo.InstallationTaskInstallerVO;
import my.hive.domain.installation.model.vo.InstallationTaskVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallationTaskInstallerContractTest {

    @Test
    void requestAndResponseExposeInstallerListsInsteadOfRetiredSinglePersonFields() throws Exception {
        assertEquals(List.class, InstallationTaskStatusUpdateRequest.class.getDeclaredField("installers").getType());
        assertEquals(String.class, InstallationTaskInstallerRequest.class.getDeclaredField("name").getType());
        assertEquals(String.class, InstallationTaskInstallerRequest.class.getDeclaredField("phone").getType());

        assertEquals(List.class, InstallationTaskVO.class.getDeclaredField("installers").getType());
        assertEquals(Long.class, InstallationTaskInstallerVO.class.getDeclaredField("id").getType());
        assertEquals(String.class, InstallationTaskInstallerVO.class.getDeclaredField("name").getType());
        assertEquals(String.class, InstallationTaskInstallerVO.class.getDeclaredField("phone").getType());
        assertEquals(Integer.class, InstallationTaskInstallerVO.class.getDeclaredField("sortOrder").getType());

        assertThrows(NoSuchFieldException.class,
                () -> InstallationTaskStatusUpdateRequest.class.getDeclaredField("constructionPersonnel"));
        assertThrows(NoSuchFieldException.class,
                () -> InstallationTaskStatusUpdateRequest.class.getDeclaredField("constructionPhone"));
        assertThrows(NoSuchFieldException.class,
                () -> InstallationTaskVO.class.getDeclaredField("constructionPersonnel"));
        assertThrows(NoSuchFieldException.class,
                () -> InstallationTaskVO.class.getDeclaredField("constructionPhone"));
        assertThrows(NoSuchFieldException.class,
                () -> InstallationTask.class.getDeclaredField("constructionPersonnel"));
        assertThrows(NoSuchFieldException.class,
                () -> InstallationTask.class.getDeclaredField("constructionPhone"));
    }

    @Test
    void installerEntityMapsEveryTenantIsolatedDetailColumn() throws Exception {
        TableName tableName = InstallationTaskInstaller.class.getAnnotation(TableName.class);
        assertNotNull(tableName);
        assertEquals("installation_task_installer", tableName.value());

        for (String field : List.of(
                "id", "tenantCode", "installationTaskId", "installerName", "installerPhone",
                "sortOrder", "createTime", "updateTime")) {
            assertNotNull(InstallationTaskInstaller.class.getDeclaredField(field));
        }
        assertTrue(com.baomidou.mybatisplus.core.mapper.BaseMapper.class.isAssignableFrom(
                my.hive.domain.installation.mapper.InstallationTaskInstallerMapper.class));
    }
}
