package my.hive.domain.inventory.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import my.hive.domain.inventory.model.entity.Cloth;
import my.hive.domain.inventory.model.vo.InventoryModelSummaryVO;
import my.hive.domain.inventory.model.vo.InventorySummaryVO;
import my.hive.domain.inventory.model.vo.InventoryTrendVO;
import my.hive.domain.inventory.model.vo.InventoryWarningVO;
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

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "SELECT COALESCE(SUM(remaining_meters), 0) AS totalMeters, COUNT(1) AS clothCount ",
            "FROM cloth ",
            "WHERE tenant_code = #{tenantCode} AND remaining_meters > 0"
    })
    InventorySummaryVO selectSummary(@Param("tenantCode") String tenantCode);

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "SELECT COUNT(1) FROM (",
            "SELECT model_code FROM cloth ",
            "WHERE tenant_code = #{tenantCode} AND remaining_meters > 0 ",
            "GROUP BY model_code ",
            "HAVING SUM(remaining_meters) <= #{threshold}",
            ") t"
    })
    Long countWarningModels(@Param("tenantCode") String tenantCode, @Param("threshold") BigDecimal threshold);

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "SELECT MIN(id) AS id, model_code AS modelCode, COALESCE(SUM(remaining_meters), 0) AS totalMeters, MAX(update_time) AS latestTime ",
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

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "SELECT statDate, ",
            "COALESCE(SUM(CASE WHEN operateType = 0 THEN operateMeters ELSE 0 END), 0) AS inMeters, ",
            "COALESCE(SUM(CASE WHEN operateType = 1 THEN operateMeters ELSE 0 END), 0) AS outMeters ",
            "FROM (",
            "SELECT DATE(create_time) AS statDate, operate_type AS operateType, operate_meters AS operateMeters ",
            "FROM inventory_record ",
            "WHERE tenant_code = #{tenantCode} AND create_time >= #{startTime} AND create_time < #{endTime} ",
            ") dailyRecord ",
            "GROUP BY statDate ",
            "ORDER BY statDate ASC"
    })
    List<InventoryTrendVO> selectTrend(@Param("tenantCode") String tenantCode,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    @InterceptorIgnore(tenantLine = "true")
    @Select({
            "<script>",
            "SELECT model_code AS modelCode, spec AS spec, COUNT(1) AS rollCount,",
            "       COALESCE(SUM(total_meters), 0) AS totalMeters,",
            "       COALESCE(SUM(remaining_meters), 0) AS remainingMeters,",
            "       MAX(update_time) AS latestTime",
            "FROM cloth",
            "WHERE tenant_code = #{tenantCode}",
            "<if test='status != null'> AND status = #{status}</if>",
            "<if test='status == null'> AND remaining_meters &gt; 0</if>",
            "<if test='specMin != null'> AND spec &gt;= #{specMin}</if>",
            "<if test='specMax != null'> AND spec &lt;= #{specMax}</if>",
            "<if test='updatedStart != null'> AND update_time &gt;= #{updatedStart}</if>",
            "<if test='updatedEnd != null'> AND update_time &lt; #{updatedEnd}</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (model_code LIKE CONCAT('%', #{keyword}, '%') OR barcode LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "GROUP BY model_code, spec",
            "<trim prefix='HAVING' prefixOverrides='AND'>",
            "  <if test='remainingMin != null'> AND COALESCE(SUM(remaining_meters), 0) &gt;= #{remainingMin}</if>",
            "  <if test='remainingMax != null'> AND COALESCE(SUM(remaining_meters), 0) &lt;= #{remainingMax}</if>",
            "</trim>",
            "<choose>",
            "  <when test='timeOrder == \"lifo\"'>ORDER BY MAX(COALESCE(in_time, create_time, update_time)) DESC, model_code ASC</when>",
            "  <otherwise>ORDER BY MIN(COALESCE(in_time, create_time, update_time)) ASC, model_code ASC</otherwise>",
            "</choose>",
            "</script>"
    })
    Page<InventoryModelSummaryVO> selectModelSummaryPage(Page<InventoryModelSummaryVO> page,
                                                         @Param("tenantCode") String tenantCode,
                                                         @Param("keyword") String keyword,
                                                         @Param("status") Integer status,
                                                         @Param("specMin") BigDecimal specMin,
                                                         @Param("specMax") BigDecimal specMax,
                                                         @Param("remainingMin") BigDecimal remainingMin,
                                                         @Param("remainingMax") BigDecimal remainingMax,
                                                         @Param("updatedStart") LocalDateTime updatedStart,
                                                         @Param("updatedEnd") LocalDateTime updatedEnd,
                                                         @Param("timeOrder") String timeOrder);
}