package my.management.module.ai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.ai.model.entity.AiAdviceTrainingSample;
import my.management.module.ai.model.vo.AiAdviceLearningStatVO;
import my.management.module.ai.model.vo.AiAdviceRuleDailyLearningStatVO;
import my.management.module.ai.model.vo.AiAdviceRuleLearningStatVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    /**
     * 根据样本键查询建议维度，用于反馈前做维度权限校验。
     */
    @Select("""
            SELECT category
            FROM ai_advice_training_sample
            WHERE tenant_code = #{tenantCode}
              AND sample_key = #{sampleKey}
            LIMIT 1
            """)
    String selectCategoryBySampleKey(@Param("tenantCode") String tenantCode,
                                     @Param("sampleKey") String sampleKey);

    /**
     * 按业务维度聚合租户反馈，用于后续建议的个性化排序和置信度校准。
     */
    @Select("""
            SELECT
                COALESCE(category, 'overview') AS category,
                COUNT(1) AS sampleCount,
                SUM(CASE WHEN feedback_type IS NOT NULL THEN 1 ELSE 0 END) AS feedbackCount,
                SUM(CASE WHEN feedback_type = 'useful' THEN 1 ELSE 0 END) AS positiveCount,
                SUM(CASE WHEN feedback_type = 'resolved' THEN 1 ELSE 0 END) AS resolvedCount,
                SUM(CASE WHEN feedback_type = 'irrelevant' THEN 1 ELSE 0 END) AS negativeCount,
                SUM(CASE WHEN feedback_type = 'ignored' THEN 1 ELSE 0 END) AS ignoredCount,
                COALESCE(AVG(confidence), 0) AS avgConfidence,
                MAX(feedback_time) AS latestFeedbackTime
            FROM ai_advice_training_sample
            WHERE tenant_code = #{tenantCode}
              AND update_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY COALESCE(category, 'overview')
            """)
    List<AiAdviceLearningStatVO> selectLearningStats(@Param("tenantCode") String tenantCode,
                                                     @Param("days") Integer days);

    /**
     * 按具体建议模式聚合反馈，避免整类维度被单条低质量建议误伤。
     */
    @Select("""
            SELECT
                COALESCE(category, 'overview') AS category,
                COALESCE(NULLIF(TRIM(title), ''), 'untitled') AS title,
                COALESCE(source_type, 'local_rules') AS sourceType,
                COUNT(1) AS sampleCount,
                SUM(CASE WHEN feedback_type IS NOT NULL THEN 1 ELSE 0 END) AS feedbackCount,
                SUM(CASE WHEN feedback_type = 'useful' THEN 1 ELSE 0 END) AS positiveCount,
                SUM(CASE WHEN feedback_type = 'resolved' THEN 1 ELSE 0 END) AS resolvedCount,
                SUM(CASE WHEN feedback_type = 'irrelevant' THEN 1 ELSE 0 END) AS negativeCount,
                SUM(CASE WHEN feedback_type = 'ignored' THEN 1 ELSE 0 END) AS ignoredCount,
                COALESCE(AVG(confidence), 0) AS avgConfidence,
                MAX(feedback_time) AS latestFeedbackTime
            FROM ai_advice_training_sample
            WHERE tenant_code = #{tenantCode}
              AND update_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY
                COALESCE(category, 'overview'),
                COALESCE(NULLIF(TRIM(title), ''), 'untitled'),
                COALESCE(source_type, 'local_rules')
            ORDER BY feedbackCount DESC, sampleCount DESC
            LIMIT #{limit}
            """)
    List<AiAdviceRuleLearningStatVO> selectRuleLearningStats(@Param("tenantCode") String tenantCode,
                                                             @Param("days") Integer days,
                                                             @Param("limit") Integer limit);

    /**
     * 按天聚合具体建议模式反馈，用于判断候选策略是否稳定胜出。
     */
    @Select("""
            SELECT
                DATE_FORMAT(update_time, '%Y-%m-%d') AS sampleDay,
                COALESCE(category, 'overview') AS category,
                COALESCE(NULLIF(TRIM(title), ''), 'untitled') AS title,
                COALESCE(source_type, 'local_rules') AS sourceType,
                COUNT(1) AS sampleCount,
                SUM(CASE WHEN feedback_type IS NOT NULL THEN 1 ELSE 0 END) AS feedbackCount,
                SUM(CASE WHEN feedback_type = 'useful' THEN 1 ELSE 0 END) AS positiveCount,
                SUM(CASE WHEN feedback_type = 'resolved' THEN 1 ELSE 0 END) AS resolvedCount,
                SUM(CASE WHEN feedback_type = 'irrelevant' THEN 1 ELSE 0 END) AS negativeCount,
                SUM(CASE WHEN feedback_type = 'ignored' THEN 1 ELSE 0 END) AS ignoredCount
            FROM ai_advice_training_sample
            WHERE tenant_code = #{tenantCode}
              AND update_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY
                DATE_FORMAT(update_time, '%Y-%m-%d'),
                COALESCE(category, 'overview'),
                COALESCE(NULLIF(TRIM(title), ''), 'untitled'),
                COALESCE(source_type, 'local_rules')
            ORDER BY sampleDay DESC, feedbackCount DESC, sampleCount DESC
            LIMIT #{limit}
            """)
    List<AiAdviceRuleDailyLearningStatVO> selectRuleDailyLearningStats(@Param("tenantCode") String tenantCode,
                                                                       @Param("days") Integer days,
                                                                       @Param("limit") Integer limit);
}
