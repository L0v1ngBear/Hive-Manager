package my.management.module.receipt.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.receipt.model.entity.OutboundOrder;
import my.management.module.receipt.model.vo.OutboundPrintOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
/**
 * OutboundOrderMapper 属于管理端后端打印回执模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "SELECT o.id, o.order_no AS orderNo, o.customer_name AS customerName, o.create_time AS createTime, ",
            "COALESCE(u.name, CONCAT('用户', o.operator_id)) AS operator, ",
            "COUNT(i.id) AS itemCount, COALESCE(SUM(i.meters), 0) AS totalMeters ",
            "FROM outbound_order o ",
            "LEFT JOIN outbound_item i ON i.order_id = o.id AND i.tenant_code = o.tenant_code ",
            "LEFT JOIN user u ON u.id = o.operator_id AND u.tenant_code = o.tenant_code ",
            "WHERE o.tenant_code = #{tenantCode} AND o.order_status = 1 AND o.print_status = 0 ",
            "GROUP BY o.id, o.order_no, o.customer_name, o.create_time, u.name, o.operator_id ",
            "ORDER BY o.update_time ASC, o.id ASC"
    })
    List<OutboundPrintOrderVO> selectPendingPrintList(@Param("tenantCode") String tenantCode);
}
