package my.management.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.inventory.model.entity.Cloth;
import my.management.module.inventory.model.vo.InventorySummaryVO;
import my.management.module.inventory.model.vo.InventoryTrendVO;
import my.management.module.inventory.model.vo.InventoryWarningVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 布匹库存数据访问层。
 */
@Mapper
public interface ClothMapper extends BaseMapper<Cloth> {

    @Select({
            "SELECT COALESCE(SUM(remaining_meters), 0) AS totalMeters, COUNT(1) AS clothCount ",
            "FROM cloth ",
            "WHERE tenant_code = #{tenantCode} AND remaining_meters > 0"
    })
    InventorySummaryVO selectSummary(@Param("tenantCode") String tenantCode);

    @Select({
            "SELECT COUNT(1) FROM (",
            "SELECT model_code FROM cloth ",
            "WHERE tenant_code = #{tenantCode} AND remaining_meters > 0 ",
            "GROUP BY model_code ",
            "HAVING SUM(remaining_meters) <= #{threshold}",
            ") t"
    })
    Long countWarningModels(@Param("tenantCode") String tenantCode, @Param("threshold") BigDecimal threshold);

    @Select({
            "SELECT model_code AS modelCode, COALESCE(SUM(remaining_meters), 0) AS totalMeters, MAX(update_time) AS latestTime ",
            "FROM cloth ",
            "WHERE tenant_code = #{tenantCode} AND remaining_meters > 0 ",
            "GROUP BY model_code ",
            "HAVING SUM(remaining_meters) <= #{threshold} ",
            "ORDER BY totalMeters ASC, latestTime DESC ",
            "LIMIT #{limit}"
    })
    List<InventoryWarningVO> selectWarnings(@Param("tenantCode") String tenantCode,
                                            @Param("threshold") BigDecimal threshold,
                                            @Param("limit") Integer limit);

    @Select({
            "SELECT DATE(create_time) AS statDate, ",
            "COALESCE(SUM(CASE WHEN operate_type = 0 THEN operate_meters ELSE 0 END), 0) AS inMeters, ",
            "COALESCE(SUM(CASE WHEN operate_type = 1 THEN operate_meters ELSE 0 END), 0) AS outMeters ",
            "FROM inventory_record ",
            "WHERE tenant_code = #{tenantCode} AND create_time >= #{startTime} AND create_time < #{endTime} ",
            "GROUP BY DATE(create_time) ",
            "ORDER BY statDate ASC"
    })
    List<InventoryTrendVO> selectTrend(@Param("tenantCode") String tenantCode,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
