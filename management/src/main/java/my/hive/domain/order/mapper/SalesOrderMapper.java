package my.hive.domain.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.order.model.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 销售订单主表数据访问层。
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT order_id AS orderId,
                   tenant_code AS tenantCode,
                   status,
                   order_category AS orderCategory,
                   customer_name AS customerName,
                   customer_phone AS customerPhone,
                   project_name AS projectName,
                   brand_name AS brandName,
                   goods_desc AS goodsDesc,
                   total_quantity AS totalQuantity,
                   information_channel AS informationChannel,
                   is_invoice AS isInvoice,
                   creator,
                   attachment_name AS attachmentName,
                   attachment_url AS attachmentUrl,
                   attachment_size AS attachmentSize,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM sales_order
            WHERE tenant_code = #{tenantCode}
              AND order_id = #{orderId}
            LIMIT 1
            FOR UPDATE
            """)
    SalesOrder selectByOrderIdForUpdate(@Param("tenantCode") String tenantCode,
                                        @Param("orderId") String orderId);
}
