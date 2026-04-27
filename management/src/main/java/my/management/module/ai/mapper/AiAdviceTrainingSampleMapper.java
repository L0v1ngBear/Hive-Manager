package my.management.module.ai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.ai.model.entity.AiAdviceTrainingSample;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AI 建议训练样本数据访问层。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AiAdviceTrainingSampleMapper {

    @Insert("""
            <script>
            INSERT INTO ai_advice_training_sample (
                tenant_code, sample_key, category, title, source_type, priority, confidence,
                input_snapshot_json, behavior_context_json, advice_json, label_status,
                occurrence_count, create_time, update_time
            ) VALUES
            <foreach collection="items" item="item" separator=",">
            (
                #{item.tenantCode}, #{item.sampleKey}, #{item.category}, #{item.title},
                #{item.sourceType}, #{item.priority}, #{item.confidence},
                #{item.inputSnapshotJson}, #{item.behaviorContextJson}, #{item.adviceJson},
                #{item.labelStatus}, 1, NOW(), NOW()
            )
            </foreach>
            ON DUPLICATE KEY UPDATE
                source_type = VALUES(source_type),
                priority = VALUES(priority),
                confidence = VALUES(confidence),
                input_snapshot_json = VALUES(input_snapshot_json),
                behavior_context_json = VALUES(behavior_context_json),
                advice_json = VALUES(advice_json),
                occurrence_count = occurrence_count + 1,
                update_time = NOW()
            </script>
            """)
    int upsertBatch(@Param("items") List<AiAdviceTrainingSample> items);

    /**
     * 回写用户反馈标签。
     *
     * <p>这里只允许按当前租户和样本键更新，避免误把其他租户的训练样本打标。</p>
     */
    @Update("""
            UPDATE ai_advice_training_sample
            SET label_status = #{labelStatus},
                feedback_type = #{feedbackType},
                feedback_text = #{feedbackText},
                feedback_user_id = #{feedbackUserId},
                feedback_time = NOW(),
                update_time = NOW()
            WHERE tenant_code = #{tenantCode}
              AND sample_key = #{sampleKey}
            """)
    int updateFeedback(@Param("tenantCode") String tenantCode,
                       @Param("sampleKey") String sampleKey,
                       @Param("labelStatus") String labelStatus,
                       @Param("feedbackType") String feedbackType,
                       @Param("feedbackText") String feedbackText,
                       @Param("feedbackUserId") Long feedbackUserId);
}
