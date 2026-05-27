package my.management.module.ai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.ai.model.vo.BadProductTypeSummaryRowVO;
import my.management.module.ai.model.vo.CustomerOrderDigestRowVO;
import my.management.module.ai.model.vo.CustomerValueSummaryRowVO;
import my.management.module.ai.model.vo.DueOrderRiskRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI 分析数据访问层，负责准备规则分析所需的聚合原始数据。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AiAnalysisMapper {

    @Select({
            "SELECT customer_name AS customerName, create_time AS createTime, total_amount AS totalAmount ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND customer_name IS NOT NULL AND customer_name <> '' ",
            "AND create_time >= #{startTime} ",
            "ORDER BY customer_name ASC, create_time ASC"
    })
    List<CustomerOrderDigestRowVO> selectCustomerOrderDigests(@Param("tenantCode") String tenantCode,
                                                              @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT customer_name AS customerName, COUNT(1) AS orderCount, COALESCE(SUM(total_amount), 0) AS totalAmount ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND customer_name IS NOT NULL AND customer_name <> '' ",
            "AND create_time >= #{startTime} ",
            "GROUP BY customer_name ",
            "ORDER BY totalAmount DESC, orderCount DESC ",
            "LIMIT 1"
    })
    CustomerValueSummaryRowVO selectTopCustomerValueSince(@Param("tenantCode") String tenantCode,
                                                         @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT customer_name AS customerName, create_time AS createTime, total_amount AS totalAmount ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND customer_name = #{customerName} ",
            "AND create_time >= #{startTime} ",
            "ORDER BY create_time ASC"
    })
    List<CustomerOrderDigestRowVO> selectCustomerOrderDigestsByCustomerName(@Param("tenantCode") String tenantCode,
                                                                            @Param("customerName") String customerName,
                                                                            @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT type, COUNT(1) AS recordCount, COALESCE(SUM(loss_amount), 0) AS totalLossAmount ",
            "FROM bad_product_record ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime} ",
            "GROUP BY type ",
            "ORDER BY recordCount DESC, totalLossAmount DESC ",
            "LIMIT 1"
    })
    BadProductTypeSummaryRowVO selectTopBadProductType(@Param("tenantCode") String tenantCode,
                                                       @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT order_id AS orderId, customer_name AS customerName, status, delivery_date AS deliveryDate ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND delivery_date IS NOT NULL AND delivery_date <> '' ",
            "AND delivery_date <= #{deliveryDateLimit} ",
            "AND status NOT IN ('shipped', 'completed') ",
            "ORDER BY delivery_date ASC, update_time DESC ",
            "LIMIT #{limit}"
    })
    List<DueOrderRiskRowVO> selectDueSalesOrders(@Param("tenantCode") String tenantCode,
                                                 @Param("deliveryDateLimit") String deliveryDateLimit,
                                                 @Param("limit") Integer limit);

    @Select({
            "SELECT COUNT(1) ",
            "FROM bad_product_record ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    Long countBadProductRecordsBetween(@Param("tenantCode") String tenantCode,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    Long countSalesOrdersBetween(@Param("tenantCode") String tenantCode,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT COALESCE(SUM(total_amount), 0) ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    BigDecimal sumSalesAmountBetween(@Param("tenantCode") String tenantCode,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM production_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    Long countProductionOrdersBetween(@Param("tenantCode") String tenantCode,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT COALESCE(SUM(loss_amount), 0) ",
            "FROM bad_product_record ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    BigDecimal sumBadProductLossBetween(@Param("tenantCode") String tenantCode,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT COUNT(DISTINCT customer_name) ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND customer_name IS NOT NULL AND customer_name <> '' ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    Long countActiveCustomersBetween(@Param("tenantCode") String tenantCode,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM customer ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    Long countNewCustomersBetween(@Param("tenantCode") String tenantCode,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime}"
    })
    Long countSalesOrdersSince(@Param("tenantCode") String tenantCode,
                               @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT COALESCE(SUM(total_amount), 0) ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime}"
    })
    BigDecimal sumSalesAmountSince(@Param("tenantCode") String tenantCode,
                                   @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM production_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime}"
    })
    Long countProductionOrdersSince(@Param("tenantCode") String tenantCode,
                                    @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM production_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND status = 'producing'"
    })
    Long countProducingOrders(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND delivery_date IS NOT NULL AND delivery_date <> '' ",
            "AND delivery_date <= #{deliveryDateLimit} ",
            "AND status NOT IN ('shipped', 'completed')"
    })
    Long countDueSoonUnshippedOrders(@Param("tenantCode") String tenantCode,
                                     @Param("deliveryDateLimit") String deliveryDateLimit);

    @Select({
            "SELECT COALESCE(SUM(remaining_meters), 0) ",
            "FROM cloth ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND del_flag = 0"
    })
    BigDecimal sumInventoryMeters(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) FROM (",
            "  SELECT model_code ",
            "  FROM cloth ",
            "  WHERE tenant_code = #{tenantCode} AND del_flag = 0 ",
            "  GROUP BY model_code ",
            "  HAVING COALESCE(SUM(remaining_meters), 0) < #{threshold}",
            ") t"
    })
    Long countLowStockModels(@Param("tenantCode") String tenantCode,
                             @Param("threshold") BigDecimal threshold);

    @Select({
            "SELECT COUNT(1) ",
            "FROM cloth ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND del_flag = 0 ",
            "AND is_bad = 1"
    })
    Long countBadCloth(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COALESCE(SUM(loss_amount), 0) ",
            "FROM bad_product_record ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime}"
    })
    BigDecimal sumBadProductLossSince(@Param("tenantCode") String tenantCode,
                                      @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM bad_product_record ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND status = 'pending'"
    })
    Long countPendingBadProduct(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) ",
            "FROM customer ",
            "WHERE tenant_code = #{tenantCode}"
    })
    Long countCustomers(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) ",
            "FROM customer ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime}"
    })
    Long countNewCustomersSince(@Param("tenantCode") String tenantCode,
                                @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT COUNT(DISTINCT customer_name) ",
            "FROM sales_order ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND customer_name IS NOT NULL AND customer_name <> '' ",
            "AND create_time >= #{startTime}"
    })
    Long countActiveCustomersSince(@Param("tenantCode") String tenantCode,
                                   @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM user ",
            "WHERE tenant_code = #{tenantCode}"
    })
    Long countEmployees(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) ",
            "FROM user ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND (status IS NULL OR status = 1) ",
            "AND COALESCE(attendance_required, 1) = 1"
    })
    Long countActiveEmployees(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) ",
            "FROM user ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND (status IS NULL OR status = 1) ",
            "AND (manager_id IS NULL OR manager_id = 0)"
    })
    Long countEmployeesWithoutManager(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) ",
            "FROM attendance_record a ",
            "INNER JOIN user u ON u.id = a.user_id AND u.tenant_code = a.tenant_code AND COALESCE(u.attendance_required, 1) = 1 ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND (a.sign_in_status IN (1, 3, 6) OR a.sign_out_status IN (2, 3, 6))"
    })
    Long countTodayAttendanceExceptions(@Param("tenantCode") String tenantCode,
                                        @Param("dayPrefix") String dayPrefix);

    @Select({
            "SELECT COUNT(1) ",
            "FROM attendance_record a ",
            "INNER JOIN user u ON u.id = a.user_id AND u.tenant_code = a.tenant_code AND COALESCE(u.attendance_required, 1) = 1 ",
            "WHERE a.tenant_code = #{tenantCode} ",
            "AND a.punch_id LIKE CONCAT(#{dayPrefix}, '%') ",
            "AND a.sign_in_status = 1"
    })
    Long countTodayLate(@Param("tenantCode") String tenantCode,
                        @Param("dayPrefix") String dayPrefix);

    @Select({
            "SELECT COUNT(1) ",
            "FROM user_leave ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND status = 1"
    })
    Long countPendingLeaveApprovals(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) ",
            "FROM user_leave ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND create_time >= #{startTime}"
    })
    Long countLeaveRequestsSince(@Param("tenantCode") String tenantCode,
                                 @Param("startTime") LocalDateTime startTime);

    @Select({
            "SELECT COUNT(1) ",
            "FROM finance_approval ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND status = 1"
    })
    Long countPendingFinanceApprovals(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COALESCE(SUM(amount), 0) ",
            "FROM finance_approval ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND status = 1"
    })
    BigDecimal sumPendingFinanceAmount(@Param("tenantCode") String tenantCode);
}
