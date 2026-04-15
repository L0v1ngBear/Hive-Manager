package my.management.module.dashboard.mapper;

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

@Mapper
public interface DashboardMapper {

    @Select("SELECT COUNT(1) FROM sales_order WHERE tenant_code = #{tenantCode} AND create_time >= #{startOfMonth}")
    Long countMonthSalesOrders(@Param("tenantCode") String tenantCode, @Param("startOfMonth") LocalDateTime startOfMonth);

    @Select("SELECT COUNT(1) FROM production_order WHERE tenant_code = #{tenantCode} AND create_time >= #{startOfMonth}")
    Long countMonthProductionOrders(@Param("tenantCode") String tenantCode, @Param("startOfMonth") LocalDateTime startOfMonth);

    @Select("SELECT COALESCE(SUM(remaining_meters), 0) FROM cloth WHERE tenant_code = #{tenantCode} AND remaining_meters > 0")
    BigDecimal sumInventoryMeters(@Param("tenantCode") String tenantCode);

    @Select("SELECT COUNT(1) FROM user_leave WHERE tenant_code = #{tenantCode} AND auditor_id = #{userId} AND status = 1")
    Long countPendingLeaveApprovals(@Param("tenantCode") String tenantCode, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM finance_approval WHERE tenant_code = #{tenantCode} AND auditor_id = #{userId} AND status = 1")
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

    @Select({
            "SELECT user_id AS userId, sign_in_status AS signInStatus, sign_out_status AS signOutStatus, ",
            "create_time AS createTime, update_time AS updateTime ",
            "FROM attendance_record ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND (sign_in_status IN (1, 3, 6) OR sign_out_status IN (2, 3, 6)) ",
            "ORDER BY update_time DESC, id DESC ",
            "LIMIT #{limit}"
    })
    List<DashboardAttendanceAlertRowVO> selectTodayAttendanceAlerts(@Param("tenantCode") String tenantCode,
                                                                    @Param("dayPrefix") String dayPrefix,
                                                                    @Param("limit") Integer limit);
}
