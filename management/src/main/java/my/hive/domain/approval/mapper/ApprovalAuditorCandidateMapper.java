package my.hive.domain.approval.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.approval.model.entity.ApprovalAuditorCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ApprovalAuditorCandidateMapper extends BaseMapper<ApprovalAuditorCandidate> {

    /**
     * Finds legacy order approval groups that became complete after their mode was changed to OR.
     * The query deliberately requires both an approved and a pending candidate so already-finished
     * or rejected-only histories are never replayed.
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT MIN(id) AS id,
                   tenant_code AS tenantCode,
                   approval_type AS approvalType,
                   approval_code AS approvalCode,
                   MIN(CASE WHEN audit_status = 1 THEN auditor_id END) AS auditorId,
                   'OR' AS approvalMode,
                   1 AS status,
                   0 AS auditStatus,
                   MIN(create_time) AS createTime,
                   MAX(update_time) AS updateTime
            FROM approval_auditor_candidate
            WHERE approval_type = 'ORDER'
              AND approval_mode = 'OR'
              AND status = 1
              AND (approval_code LIKE 'sales:%' OR approval_code LIKE 'production:%')
            GROUP BY tenant_code, approval_type, approval_code
            HAVING SUM(CASE WHEN audit_status = 1 THEN 1 ELSE 0 END) > 0
               AND SUM(CASE WHEN audit_status = 0 THEN 1 ELSE 0 END) > 0
            ORDER BY MIN(id)
            """)
    List<ApprovalAuditorCandidate> selectOrderGroupsReadyForOrReconciliation();

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id,
                   tenant_code AS tenantCode,
                   approval_type AS approvalType,
                   approval_code AS approvalCode,
                   auditor_id AS auditorId,
                   approval_mode AS approvalMode,
                   status,
                   audit_status AS auditStatus,
                   audit_comment AS auditComment,
                   audit_time AS auditTime,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM approval_auditor_candidate
            WHERE tenant_code = #{tenantCode}
              AND approval_type = #{approvalType}
              AND approval_code = #{approvalCode}
            ORDER BY id
            FOR UPDATE
            """)
    List<ApprovalAuditorCandidate> selectApprovalForUpdate(@Param("tenantCode") String tenantCode,
                                                            @Param("approvalType") String approvalType,
                                                            @Param("approvalCode") String approvalCode);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE approval_auditor_candidate
            SET audit_status = #{auditStatus},
                audit_comment = #{comment},
                audit_time = #{auditTime},
                update_time = #{auditTime}
            WHERE tenant_code = #{tenantCode}
              AND approval_type = #{approvalType}
              AND approval_code = #{approvalCode}
              AND auditor_id = #{auditorId}
              AND status = 1
              AND audit_status = 0
            """)
    int updatePendingDecision(@Param("tenantCode") String tenantCode,
                              @Param("approvalType") String approvalType,
                              @Param("approvalCode") String approvalCode,
                              @Param("auditorId") Long auditorId,
                              @Param("auditStatus") int auditStatus,
                              @Param("comment") String comment,
                              @Param("auditTime") LocalDateTime auditTime);
}
