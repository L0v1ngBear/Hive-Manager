package my.hive.domain.inventory.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.inventory.model.entity.InventorySetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface InventorySettingMapper extends BaseMapper<InventorySetting> {

    @Select("""
            SELECT id, tenant_code AS tenantCode, warning_threshold_meters AS warningThresholdMeters,
                   create_time AS createTime, update_time AS updateTime
            FROM inventory_setting
            WHERE tenant_code = #{tenantCode}
            LIMIT 1
            """)
    InventorySetting selectByTenantCode(@Param("tenantCode") String tenantCode);

    @Insert("""
            INSERT INTO inventory_setting (tenant_code, warning_threshold_meters, create_time, update_time)
            VALUES (#{tenantCode}, #{threshold}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                warning_threshold_meters = VALUES(warning_threshold_meters),
                update_time = NOW()
            """)
    int upsertWarningThreshold(@Param("tenantCode") String tenantCode,
                               @Param("threshold") BigDecimal threshold);
}