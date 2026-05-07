package my.management.module.ai.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.event.SystemEventPublisher;
import my.management.module.ai.service.AiAdviceSnapshotService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAdviceSnapshotJobHandler {

    private final AiAdviceSnapshotService aiAdviceSnapshotService;
    private final SystemEventPublisher systemEventPublisher;

    @XxlJob("aiAdviceSnapshotRefreshJob")
    public void refreshAiAdviceSnapshots() {
        try {
            Map<String, Object> result = aiAdviceSnapshotService.refreshAllTenants();
            XxlJobHelper.log("AI advice snapshot refresh finished: {}", result);
            if (Boolean.TRUE.equals(result.get("skipped"))) {
                XxlJobHelper.handleSuccess("AI advice snapshot refresh skipped: " + result.get("reason"));
            }
        } catch (Exception ex) {
            log.error("ai advice snapshot refresh job failed", ex);
            XxlJobHelper.log("AI advice snapshot refresh failed: {}", ex.getMessage());
            systemEventPublisher.error("AI_ADVICE_SNAPSHOT_REFRESH_FAILED",
                    "AI advice snapshot refresh failed",
                    ex,
                    Map.of("job", "aiAdviceSnapshotRefreshJob"));
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }
}
