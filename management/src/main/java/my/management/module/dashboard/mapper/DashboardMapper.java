package my.management.module.dashboard.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import my.management.module.dashboard.model.vo.DashboardAttendanceAlertRowVO;
import my.management.module.dashboard.model.vo.DashboardInventoryTrendRowVO;
import my.management.module.dashboard.model.vo.DashboardInventoryWarningRowVO;
import my.management.module.dashboard.model.vo.DashboardPendingPrintRowVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * DashboardMapper 属于管理端后端总览大盘模块，是数据访问类，负责与数据库交互。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface DashboardMapper {

    @Select("SELECT COUNT(1) FROM sales_order WHERE tenant_code = #{tenantCode} AND create_time >= #{startOfMonth}")
    Long countMonthSalesOrders(@Param("tenantCode") String tenantCode, @Param("startOfMonth") LocalDateTime startOfMonth);

    @Select("SELECT COUNT(1) FROM production_order WHERE tenant_code = #{tenantCode} AND create_time >= #{startOfMonth}")
    Long countMonthProductionOrders(@Param("tenantCode") String tenantCode, @Param("startOfMonth") LocalDateTime startOfMonth);

    @Select("SELECT COALESCE(SUM(remaining_meters), 0) FROM cloth WHERE tenant_code = #{tenantCode} AND remaining_meters > 0")
    BigDecimal sumInventoryMeters(@Param("tenantCode") String tenantCode);

    @Select("SELECT COUNT(1) FROM user_leave WHERE tenant_code = #{tenantCode} AND status = 1 AND (auditor_id = #{userId} OR FIND_IN_SET(#{userId}, auditor_ids) > 0)")
    Long countPendingLeaveApprovals(@Param("tenantCode") String tenantCode, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM finance_approval WHERE tenant_code = #{tenantCode} AND status = 1 AND (auditor_id = #{userId} OR FIND_IN_SET(#{userId}, auditor_ids) > 0)")
    Long countPendingFinanceApprovals(@Param("tenantCode") String tenantCode, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM outbound_order WHERE tenant_code = #{tenantCode} AND order_status = 1 AND print_status = 0")
    Long countPendingPrintOrders(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT stat_date AS statDate, ",
            "COALESCE(day_in_meters, 0) AS dayInMeters, ",
            "COALESCE(day_out_meters, 0) AS dayOutMeters ",
            "FROM inventory_statics ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND stat_date BETWEEN #{startDate} AND #{endDate} ",
            "ORDER BY stat_date ASC"
    })
    List<DashboardInventoryTrendRowVO> selectInventoryTrend(@Param("tenantCode") String tenantCode,
                                                            @Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);

    @Select({
            "SELECT statDate, ",
            "COALESCE(SUM(CASE WHEN operateType IN (#{inType}, #{externalImportType}) THEN operateMeters ELSE 0 END), 0) AS dayInMeters, ",
            "COALESCE(SUM(CASE WHEN operateType = #{outType} THEN operateMeters ELSE 0 END), 0) AS dayOutMeters ",
            "FROM (",
            "SELECT CAST(DATE(create_time) AS DATETIME) AS statDate, ",
            "operate_type AS operateType, operate_meters AS operateMeters ",
            "FROM inventory_record ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startDate} ",
            "AND create_time <= #{endDate} ",
            ") dailyRecord ",
            "GROUP BY statDate ",
            "ORDER BY statDate ASC"
    })
    List<DashboardInventoryTrendRowVO> selectInventoryRecordTrend(@Param("tenantCode") String tenantCode,
                                                                  @Param("startDate") LocalDateTime startDate,
                                                                  @Param("endDate") LocalDateTime endDate,
                                                                  @Param("inType") Integer inType,
                                                                  @Param("externalImportType") Integer externalImportType,
                                                                  @Param("outType") Integer outType);

    @Select({
            "SELECT COUNT(1) FROM (",
            "SELECT model_code ",
            "FROM cloth ",
            "WHERE tenant_code = #{tenantCode} AND remaining_meters > 0 ",
            "GROUP BY model_code ",
            "HAVING SUM(remaining_meters) <= #{threshold}",
            ") t"
    })
    Long countLowStockModels(@Param("tenantCode") String tenantCode, @Param("threshold") BigDecimal threshold);

    @Select({
            "SELECT model_code AS modelCode, ",
            "COALESCE(SUM(remaining_meters), 0) AS totalMeters, ",
            "MAX(update_time) AS latestTime ",
            "FROM cloth ",
            "WHERE tenant_code = #{tenantCode} AND remaining_meters > 0 ",
            "GROUP BY model_code ",
            "HAVING SUM(remaining_meters) <= #{threshold} ",
            "ORDER BY totalMeters ASC, latestTime DESC ",
            "LIMIT #{limit}"
    })
    List<DashboardInventoryWarningRowVO> selectLowStockModels(@Param("tenantCode") String tenantCode,
                                                              @Param("threshold") BigDecimal threshold,
                                                              @Param("limit") Integer limit);

    @Select({
            "SELECT order_no AS orderNo, customer_name AS customerName, update_time AS updateTime ",
            "FROM outbound_order ",
            "WHERE tenant_code = #{tenantCode} AND order_status = 1 AND print_status = 0 ",
            "ORDER BY update_time DESC, id DESC ",
            "LIMIT #{limit}"
    })
    List<DashboardPendingPrintRowVO> selectRecentPendingPrintOrders(@Param("tenantCode") String tenantCode,
                                                                    @Param("limit") Integer limit);

    @Select("SELECT COUNT(1) FROM user u WHERE u.tenant_code = #{tenantCode} AND (u.status IS NULL OR u.status IN (1, 2)) AND COALESCE(u.attendance_required, 1) = 1")
    Long countAttendanceEmployees(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(DISTINCT a.user_id) FROM attendance_record a ",
            "INNER JOIN user u ON u.id = a.user_id AND u.tenant_code = a.tenant_code AND COALESCE(u.attendance_required, 1) = 1 ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND (a.sign_in_status IS NOT NULL OR a.sign_out_status IS NOT NULL)"
    })
    Long countTodayAttendanceActual(@Param("tenantCode") String tenantCode,
                                    @Param("dayPrefix") String dayPrefix);

    @Select({
            "SELECT COUNT(DISTINCT a.user_id) FROM attendance_record a ",
            "INNER JOIN user u ON u.id = a.user_id AND u.tenant_code = a.tenant_code AND COALESCE(u.attendance_required, 1) = 1 ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND (a.sign_in_status IN (1, 3, 6) OR a.sign_out_status IN (2, 3, 6))"
    })
    Long countTodayAttendanceAbnormal(@Param("tenantCode") String tenantCode,
                                      @Param("dayPrefix") String dayPrefix);

    @Select({
            "SELECT a.user_id AS userId, u.name AS userName, u.department_name AS departmentName, ",
            "a.sign_in_status AS signInStatus, a.sign_out_status AS signOutStatus, ",
            "a.create_time AS createTime, a.update_time AS updateTime ",
            "FROM attendance_record a ",
            "LEFT JOIN user u ON u.id = a.user_id AND u.tenant_code = a.tenant_code ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND COALESCE(u.attendance_required, 1) = 1 ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND (a.sign_in_status IN (1, 3, 6) OR a.sign_out_status IN (2, 3, 6)) ",
            "ORDER BY a.update_time DESC, a.id DESC ",
            "LIMIT #{limit}"
    })
    List<DashboardAttendanceAlertRowVO> selectTodayAttendanceAlerts(@Param("tenantCode") String tenantCode,
                                                                    @Param("dayPrefix") String dayPrefix,
                                                                    @Param("limit") Integer limit);
}
