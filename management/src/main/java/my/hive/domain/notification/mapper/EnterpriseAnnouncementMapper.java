package my.hive.domain.notification.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.hive.domain.notification.model.entity.EnterpriseAnnouncement;
import my.hive.domain.notification.model.vo.NotificationReceiverVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 企业公告数据访问层，公告与通知待办表物理隔离。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface EnterpriseAnnouncementMapper {

    String ANNOUNCEMENT_COLUMNS = """
            id, tenant_code AS tenantCode, announcement_code AS announcementCode, title, content,
            level, route, status, publisher_user_id AS publisherUserId, publisher_name AS publisherName,
            create_time AS createTime, update_time AS updateTime
            """;

    @Insert("""
            INSERT INTO enterprise_announcement (
                tenant_code, announcement_code, title, content, level, route,
                status, publisher_user_id, publisher_name, create_time, update_time
            ) VALUES (
                #{item.tenantCode}, #{item.announcementCode}, #{item.title}, #{item.content},
                #{item.level}, #{item.route}, #{item.status}, #{item.publisherUserId},
                #{item.publisherName}, NOW(), NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "item.id", keyColumn = "id")
    int insertAnnouncement(@Param("item") EnterpriseAnnouncement item);

    @Select("""
            <script>
            SELECT
            """ + ANNOUNCEMENT_COLUMNS + """
            FROM enterprise_announcement
            WHERE BINARY tenant_code = BINARY #{tenantCode}
              AND status = 1
              <if test="levels != null and levels.size() > 0">
              AND level IN
              <foreach collection="levels" item="level" open="(" separator="," close=")">
                #{level}
              </foreach>
              </if>
            ORDER BY update_time DESC, id DESC
            LIMIT #{limit}
            </script>
            """)
    List<EnterpriseAnnouncement> selectRecentAnnouncements(@Param("tenantCode") String tenantCode,
                                                           @Param("levels") List<String> levels,
                                                           @Param("limit") Integer limit);

    @Select("""
            SELECT
                u.id AS userId,
                u.name AS userName,
                u.department_name AS departmentName,
                u.position AS positionName
            FROM user u
            LEFT JOIN emp_employee_ext ext
              ON ext.user_id = u.id
             AND BINARY ext.tenant_code = BINARY u.tenant_code
             AND IFNULL(ext.is_deleted, 0) = 0
            WHERE BINARY u.tenant_code = BINARY #{tenantCode}
              AND IFNULL(u.status, 1) = 1
              AND (ext.id IS NULL OR IFNULL(ext.is_deleted, 0) = 0)
            ORDER BY COALESCE(u.role_level, 0) DESC, u.id ASC
            """)
    List<NotificationReceiverVO> selectAnnouncementTargetUsers(@Param("tenantCode") String tenantCode);

    @Insert("""
            INSERT INTO enterprise_announcement_read (
                tenant_code, announcement_id, announcement_code, user_id, user_name,
                department_name, position_name, read_flag, read_time, create_time, update_time
            ) VALUES (
                #{tenantCode}, #{announcementId}, #{announcementCode}, #{receiver.userId},
                #{receiver.userName}, #{receiver.departmentName}, #{receiver.positionName},
                #{readFlag}, CASE WHEN #{readFlag} = 1 THEN NOW() ELSE NULL END, NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                user_name = VALUES(user_name),
                department_name = VALUES(department_name),
                position_name = VALUES(position_name),
                read_time = CASE
                    WHEN enterprise_announcement_read.read_flag = 1 THEN enterprise_announcement_read.read_time
                    WHEN VALUES(read_flag) = 1 THEN NOW()
                    ELSE enterprise_announcement_read.read_time
                END,
                read_flag = GREATEST(COALESCE(enterprise_announcement_read.read_flag, 0), COALESCE(VALUES(read_flag), 0)),
                update_time = NOW()
            """)
    int upsertReceiver(@Param("tenantCode") String tenantCode,
                       @Param("announcementId") Long announcementId,
                       @Param("announcementCode") String announcementCode,
                       @Param("receiver") NotificationReceiverVO receiver,
                       @Param("readFlag") Integer readFlag);

    @Select("""
            SELECT
                u.id AS userId,
                u.name AS userName,
                u.department_name AS departmentName,
                u.position AS positionName,
                COALESCE(r.read_flag, 0) AS readFlag,
                r.read_time AS readTime
            FROM user u
            LEFT JOIN emp_employee_ext ext
              ON ext.user_id = u.id
             AND BINARY ext.tenant_code = BINARY u.tenant_code
             AND IFNULL(ext.is_deleted, 0) = 0
            LEFT JOIN enterprise_announcement_read r
              ON BINARY r.tenant_code = BINARY u.tenant_code
             AND r.announcement_id = #{announcementId}
             AND r.user_id = u.id
            WHERE BINARY u.tenant_code = BINARY #{tenantCode}
              AND IFNULL(u.status, 1) = 1
              AND (ext.id IS NULL OR IFNULL(ext.is_deleted, 0) = 0)
            ORDER BY COALESCE(r.read_flag, 0) ASC, u.department_name ASC, u.name ASC, u.id ASC
            """)
    List<NotificationReceiverVO> selectReceiverStatuses(@Param("tenantCode") String tenantCode,
                                                        @Param("announcementId") Long announcementId);

    @Update("""
            UPDATE enterprise_announcement_read
            SET read_flag = 1,
                read_time = COALESCE(read_time, NOW()),
                update_time = NOW()
            WHERE BINARY tenant_code = BINARY #{tenantCode}
              AND announcement_id = #{announcementId}
              AND user_id = #{userId}
            """)
    int markRead(@Param("tenantCode") String tenantCode,
                 @Param("announcementId") Long announcementId,
                 @Param("userId") Long userId);
}
