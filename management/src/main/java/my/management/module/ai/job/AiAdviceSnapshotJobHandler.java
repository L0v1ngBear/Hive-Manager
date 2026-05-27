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
            XxlJobHelper.log("经营建议快照刷新完成: {}", result);
            if (Boolean.TRUE.equals(result.get("skipped"))) {
                XxlJobHelper.handleSuccess("经营建议快照刷新已跳过: " + result.get("reason"));
            }
        } catch (Exception ex) {
            log.error("ai advice snapshot refresh job failed", ex);
            XxlJobHelper.log("经营建议快照刷新失败: {}", ex.getMessage());
            systemEventPublisher.error("AI_ADVICE_SNAPSHOT_REFRESH_FAILED",
                    "经营建议快照刷新失败",
                    ex,
                    Map.of("job", "aiAdviceSnapshotRefreshJob"));
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }
}
