package my.management.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.inventory.model.entity.InventoryRecord;
import my.management.module.inventory.model.vo.InventoryRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存流水数据访问层。
 */
@Mapper
public interface InventoryRecordMapper extends BaseMapper<InventoryRecord> {

    @Select({
            "SELECT COALESCE(SUM(operate_meters), 0) FROM inventory_record ",
            "WHERE tenant_code = #{tenantCode} AND operate_type = #{operateType} ",
            "AND create_time >= #{startTime} AND create_time < #{endTime}"
    })
    BigDecimal sumOperateMeters(@Param("tenantCode") String tenantCode,
                                @Param("operateType") Integer operateType,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    @Select({
            "SELECT r.id, c.barcode, r.model_code AS modelCode, r.operate_type AS operateType, ",
            "r.operate_meters AS operateMeters, r.remaining_meters AS remainingMeters, ",
            "COALESCE(e.name, '系统') AS operatorName, r.create_time AS createTime ",
            "FROM inventory_record r ",
            "LEFT JOIN cloth c ON c.id = r.cloth_id AND c.tenant_code = r.tenant_code ",
            "LEFT JOIN user e ON e.id = r.operator_id AND e.tenant_code = r.tenant_code ",
            "WHERE r.tenant_code = #{tenantCode} ",
            "ORDER BY r.create_time DESC, r.id DESC ",
            "LIMIT #{limit}"
    })
    List<InventoryRecordVO> selectRecent(@Param("tenantCode") String tenantCode, @Param("limit") Integer limit);
}
