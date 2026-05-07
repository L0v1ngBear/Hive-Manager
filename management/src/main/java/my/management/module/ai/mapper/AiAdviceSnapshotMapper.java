package my.management.module.ai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.ai.model.entity.AiAdviceSnapshot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AiAdviceSnapshotMapper {

    @Select("""
            SELECT
                id,
                tenant_code AS tenantCode,
                snapshot_json AS snapshotJson,
                advice_count AS adviceCount,
                status,
                error_message AS errorMessage,
                generated_at AS generatedAt,
                last_attempt_time AS lastAttemptTime,
                create_time AS createTime,
                update_time AS updateTime
            FROM ai_advice_snapshot
            WHERE tenant_code = #{tenantCode}
            LIMIT 1
            """)
    AiAdviceSnapshot selectByTenantCode(@Param("tenantCode") String tenantCode);

    @Insert("""
            INSERT INTO ai_advice_snapshot (
                tenant_code, snapshot_json, advice_count, status, error_message,
                generated_at, last_attempt_time, create_time, update_time
            ) VALUES (
                #{snapshot.tenantCode}, #{snapshot.snapshotJson}, #{snapshot.adviceCount},
                'SUCCESS', NULL, NOW(), NOW(), NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                snapshot_json = VALUES(snapshot_json),
                advice_count = VALUES(advice_count),
                status = 'SUCCESS',
                error_message = NULL,
                generated_at = NOW(),
                last_attempt_time = NOW(),
                update_time = NOW()
            """)
    int upsertSuccess(@Param("snapshot") AiAdviceSnapshot snapshot);

    @Insert("""
            INSERT INTO ai_advice_snapshot (
                tenant_code, snapshot_json, advice_count, status, error_message,
                generated_at, last_attempt_time, create_time, update_time
            ) VALUES (
                #{tenantCode}, NULL, 0, 'ERROR', #{errorMessage},
                NULL, NOW(), NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                status = 'ERROR',
                error_message = VALUES(error_message),
                last_attempt_time = NOW(),
                update_time = NOW()
            """)
    int markFailure(@Param("tenantCode") String tenantCode, @Param("errorMessage") String errorMessage);
}
