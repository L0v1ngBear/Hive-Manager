package my.management.module.ai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.ai.model.vo.BadProductTypeSummaryRowVO;
import my.management.module.ai.model.vo.CustomerOrderDigestRowVO;
import my.management.module.ai.model.vo.DueOrderRiskRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
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
}
