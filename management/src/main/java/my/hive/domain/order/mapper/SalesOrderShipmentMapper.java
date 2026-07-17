package my.hive.domain.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.order.model.entity.SalesOrderShipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface SalesOrderShipmentMapper extends BaseMapper<SalesOrderShipment> {

    @Update("""
            UPDATE sales_order_shipment
            SET logistics_company = #{company},
                tracking_no = #{trackingNo},
                sort_order = #{sortOrder},
                updater = #{updater},
                updater_name = #{updaterName},
                version = version + 1,
                update_time = #{updateTime}
            WHERE id = #{id}
              AND BINARY tenant_code = BINARY #{tenantCode}
              AND BINARY order_id = BINARY #{orderId}
              AND version = #{version}
            """)
    int updateShipment(@Param("id") Long id,
                       @Param("tenantCode") String tenantCode,
                       @Param("orderId") String orderId,
                       @Param("version") Integer version,
                       @Param("company") String company,
                       @Param("trackingNo") String trackingNo,
                       @Param("sortOrder") Integer sortOrder,
                       @Param("updater") String updater,
                       @Param("updaterName") String updaterName,
                       @Param("updateTime") LocalDateTime updateTime);
}
