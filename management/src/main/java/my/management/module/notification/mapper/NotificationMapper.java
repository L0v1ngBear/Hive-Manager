package my.management.module.notification.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.notification.model.entity.NotificationRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知记录数据访问层。
 *
 * <p>这里使用显式 tenant_code 条件并忽略租户插件，避免插件自动追加条件后与广播通知
 * 或手动租户查询产生重复过滤。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface NotificationMapper {

    @Insert("""
            INSERT INTO notification_record (
                tenant_code, dedupe_key, biz_type, biz_id, title, content, level, channel,
                route, receiver_user_id, receiver_name, receiver_phone, status, read_flag,
                send_status, source_type, create_time, update_time
            ) VALUES (
                #{item.tenantCode}, #{item.dedupeKey}, #{item.bizType}, #{item.bizId},
                #{item.title}, #{item.content}, #{item.level}, #{item.channel},
                #{item.route}, #{item.receiverUserId}, #{item.receiverName}, #{item.receiverPhone},
                #{item.status}, #{item.readFlag}, #{item.sendStatus}, #{item.sourceType}, NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                content = VALUES(content),
                level = VALUES(level),
                route = VALUES(route),
                status = VALUES(status),
                source_type = VALUES(source_type),
                update_time = NOW()
            """)
    int upsert(@Param("item") NotificationRecord item);

    @Select("""
            SELECT *
            FROM notification_record
            WHERE tenant_code = #{tenantCode}
              AND status = 1
              AND read_flag = 0
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
            ORDER BY
              CASE level WHEN 'critical' THEN 1 WHEN 'warning' THEN 2 WHEN 'info' THEN 3 ELSE 4 END,
              update_time DESC
            LIMIT #{limit}
            """)
    List<NotificationRecord> selectUnread(@Param("tenantCode") String tenantCode,
                                          @Param("userId") Long userId,
                                          @Param("limit") Integer limit);

    @Select("""
            SELECT COUNT(1)
            FROM notification_record
            WHERE tenant_code = #{tenantCode}
              AND status = 1
              AND read_flag = 0
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
            """)
    Long countUnread(@Param("tenantCode") String tenantCode, @Param("userId") Long userId);

    @Select("""
            SELECT *
            FROM notification_record
            WHERE tenant_code = #{tenantCode}
              AND status = 1
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
              AND (#{onlyUnread} = false OR read_flag = 0)
            ORDER BY update_time DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<NotificationRecord> selectPage(@Param("tenantCode") String tenantCode,
                                        @Param("userId") Long userId,
                                        @Param("onlyUnread") Boolean onlyUnread,
                                        @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);

    @Select("""
            SELECT COUNT(1)
            FROM notification_record
            WHERE tenant_code = #{tenantCode}
              AND status = 1
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
              AND (#{onlyUnread} = false OR read_flag = 0)
            """)
    Long countPage(@Param("tenantCode") String tenantCode,
                   @Param("userId") Long userId,
                   @Param("onlyUnread") Boolean onlyUnread);

    @Update("""
            UPDATE notification_record
            SET read_flag = 1, read_time = NOW(), update_time = NOW()
            WHERE id = #{id}
              AND tenant_code = #{tenantCode}
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
            """)
    int markRead(@Param("tenantCode") String tenantCode, @Param("userId") Long userId, @Param("id") Long id);
}
