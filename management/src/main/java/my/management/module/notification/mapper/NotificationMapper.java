package my.management.module.notification.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.notification.model.entity.NotificationRecord;
import my.management.module.notification.model.vo.NotificationReceiverVO;
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

    String RECORD_COLUMNS = """
            id, tenant_code, dedupe_key, biz_type, biz_id, title, content, level, channel,
            route, receiver_user_id, receiver_name, receiver_phone, status, read_flag,
            read_time, send_status, send_time, task_status, close_result, close_note,
            close_user_id, close_time, source_type, create_time, update_time
            """;

    @Insert("""
            INSERT INTO notification_record (
                tenant_code, dedupe_key, biz_type, biz_id, title, content, level, channel,
                route, receiver_user_id, receiver_name, receiver_phone, status, read_flag,
                send_status, task_status, close_result, close_note, source_type, create_time, update_time
            ) VALUES (
                #{item.tenantCode}, #{item.dedupeKey}, #{item.bizType}, #{item.bizId},
                #{item.title}, #{item.content}, #{item.level}, #{item.channel},
                #{item.route}, #{item.receiverUserId}, #{item.receiverName}, #{item.receiverPhone},
                #{item.status}, #{item.readFlag}, #{item.sendStatus}, #{item.taskStatus},
                #{item.closeResult}, #{item.closeNote}, #{item.sourceType}, NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                biz_id = VALUES(biz_id),
                title = VALUES(title),
                content = VALUES(content),
                level = VALUES(level),
                route = VALUES(route),
                status = VALUES(status),
                source_type = VALUES(source_type),
                task_status = CASE
                    WHEN task_status IN ('DONE', 'IGNORED') THEN task_status
                    ELSE VALUES(task_status)
                END,
                update_time = NOW()
            """)
    int upsert(@Param("item") NotificationRecord item);

    @Insert("""
            INSERT INTO notification_record (
                tenant_code, dedupe_key, biz_type, biz_id, title, content, level, channel,
                route, receiver_user_id, receiver_name, receiver_phone, status, read_flag,
                send_status, task_status, close_result, close_note, source_type, create_time, update_time
            ) VALUES (
                #{item.tenantCode}, #{item.dedupeKey}, #{item.bizType}, #{item.bizId},
                #{item.title}, #{item.content}, #{item.level}, #{item.channel},
                #{item.route}, #{item.receiverUserId}, #{item.receiverName}, #{item.receiverPhone},
                #{item.status}, #{item.readFlag}, #{item.sendStatus}, #{item.taskStatus},
                #{item.closeResult}, #{item.closeNote}, #{item.sourceType}, NOW(), NOW()
            )
            """)
    int insertAnnouncement(@Param("item") NotificationRecord item);

    @Select("""
            SELECT
            """ + RECORD_COLUMNS + """
            FROM notification_record
            WHERE tenant_code = #{tenantCode}
              AND biz_type = 'ANNOUNCEMENT'
              AND receiver_user_id IS NULL
              AND status = 1
            ORDER BY update_time DESC
            LIMIT #{limit}
            """)
    List<NotificationRecord> selectRecentAnnouncements(@Param("tenantCode") String tenantCode,
                                                       @Param("limit") Integer limit);

    @Select("""
            SELECT
            """ + RECORD_COLUMNS + """
            FROM notification_record
            WHERE tenant_code = #{tenantCode}
              AND status = 1
              AND (
                (task_status IS NULL AND read_flag = 0)
                OR task_status IN ('PENDING', 'PROCESSING')
              )
              AND (biz_type != 'AI_ADVICE' OR receiver_user_id = #{userId})
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
              AND (
                (task_status IS NULL AND read_flag = 0)
                OR task_status IN ('PENDING', 'PROCESSING')
              )
              AND (biz_type != 'AI_ADVICE' OR receiver_user_id = #{userId})
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
            """)
    Long countUnread(@Param("tenantCode") String tenantCode, @Param("userId") Long userId);

    @Select("""
            SELECT
            """ + RECORD_COLUMNS + """
            FROM notification_record
            WHERE tenant_code = #{tenantCode}
              AND status = 1
              AND (biz_type != 'AI_ADVICE' OR receiver_user_id = #{userId})
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
              AND (biz_type != 'AI_ADVICE' OR receiver_user_id = #{userId})
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

    @Select("""
            SELECT
            """ + RECORD_COLUMNS + """
            FROM notification_record
            WHERE id = #{id}
              AND tenant_code = #{tenantCode}
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
            LIMIT 1
            """)
    NotificationRecord selectByIdForUser(@Param("tenantCode") String tenantCode,
                                         @Param("userId") Long userId,
                                         @Param("id") Long id);

    @Update("""
            UPDATE notification_record
            SET task_status = #{taskStatus},
                close_result = #{closeResult},
                close_note = #{closeNote},
                close_user_id = #{userId},
                close_time = NOW(),
                read_flag = 1,
                read_time = COALESCE(read_time, NOW()),
                update_time = NOW()
            WHERE id = #{id}
              AND tenant_code = #{tenantCode}
              AND (receiver_user_id IS NULL OR receiver_user_id = #{userId})
              AND (task_status IS NULL OR task_status NOT IN ('DONE', 'IGNORED'))
            """)
    int closeTask(@Param("tenantCode") String tenantCode,
                  @Param("userId") Long userId,
                  @Param("id") Long id,
                  @Param("taskStatus") String taskStatus,
                  @Param("closeResult") String closeResult,
                  @Param("closeNote") String closeNote);

    @Select("""
            SELECT
                u.id AS userId,
                u.name AS userName,
                GROUP_CONCAT(DISTINCT p.perm_code ORDER BY p.perm_code SEPARATOR ',') AS permissionCodes
            FROM user u
            INNER JOIN sys_user_role ur
              ON ur.user_id = u.id
             AND ur.tenant_code = u.tenant_code
             AND IFNULL(ur.is_deleted, 0) = 0
            INNER JOIN sys_role r
              ON r.id = ur.role_id
             AND r.tenant_code = u.tenant_code
             AND IFNULL(r.is_deleted, 0) = 0
            INNER JOIN sys_role_permission rp
              ON rp.role_id = r.id
             AND IFNULL(rp.is_deleted, 0) = 0
            INNER JOIN sys_permission p
              ON p.id = rp.permission_id
             AND IFNULL(p.is_deleted, 0) = 0
            WHERE u.tenant_code = #{tenantCode}
              AND IFNULL(u.status, 1) = 1
              AND p.perm_code IN (
                '*', '*:*', 'dashboard:*', 'dashboard:ai:*', 'dashboard:ai:view',
                'dashboard:ai:inventory', 'dashboard:ai:order', 'dashboard:ai:customer',
                'dashboard:ai:quality', 'dashboard:ai:finance', 'dashboard:ai:employee',
                'dashboard:ai:operation'
              )
            GROUP BY u.id, u.name
            """)
    List<NotificationReceiverVO> selectAiAdviceReceivers(@Param("tenantCode") String tenantCode);
}
