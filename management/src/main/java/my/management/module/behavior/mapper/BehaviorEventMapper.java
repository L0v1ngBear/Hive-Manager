package my.management.module.behavior.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.behavior.model.entity.BehaviorEvent;
import my.management.module.behavior.model.vo.BehaviorModulePreferenceVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户行为事件数据访问层。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface BehaviorEventMapper {

    @Insert("""
            <script>
            INSERT INTO behavior_event (
                tenant_code, user_id, event_type, page_path, module, target_type, target_id,
                action, source, session_id, metadata_json, client_time, create_time
            ) VALUES
            <foreach collection="items" item="item" separator=",">
            (
                #{item.tenantCode}, #{item.userId}, #{item.eventType}, #{item.pagePath},
                #{item.module}, #{item.targetType}, #{item.targetId}, #{item.action},
                #{item.source}, #{item.sessionId}, #{item.metadataJson}, #{item.clientTime}, NOW()
            )
            </foreach>
            </script>
            """)
    int batchInsert(@Param("items") List<BehaviorEvent> items);

    @Select("""
            SELECT
                CASE
                    WHEN event_type LIKE 'ai_advice%' AND target_id LIKE '%:%'
                        THEN SUBSTRING_INDEX(target_id, ':', 1)
                    ELSE COALESCE(module, 'unknown')
                END AS category,
                SUM(CASE
                    WHEN event_type = 'ai_advice_click' THEN 5
                    WHEN event_type = 'notification_open' THEN 4
                    WHEN event_type = 'global_search' THEN 2
                    WHEN event_type = 'ai_advice_view' THEN 1
                    WHEN event_type = 'page_view' THEN 0.5
                    ELSE 0.5
                END) AS behaviorScore,
                SUM(CASE
                    WHEN event_type IN ('ai_advice_click', 'notification_open') THEN 1
                    ELSE 0
                END) AS clickCount,
                COUNT(1) AS totalCount
            FROM behavior_event
            WHERE tenant_code = #{tenantCode}
              AND create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY category
            """)
    List<BehaviorModulePreferenceVO> selectTenantPreferences(@Param("tenantCode") String tenantCode,
                                                             @Param("days") Integer days);
}
