package my.management.module.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.order.model.entity.OrderSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface OrderSettingMapper extends BaseMapper<OrderSetting> {

    @Select("""
            SELECT id, tenant_code AS tenantCode,
                   stale_warning_days AS staleWarningDays,
                   sample_room_stale_warning_days AS sampleRoomStaleWarningDays,
                   bulk_stale_warning_days AS bulkStaleWarningDays,
                   replenishment_stale_warning_days AS replenishmentStaleWarningDays,
                   drawing_budget_stale_warning_days AS drawingBudgetStaleWarningDays,
                   create_time AS createTime, update_time AS updateTime
            FROM order_setting
            WHERE tenant_code = #{tenantCode}
            LIMIT 1
            """)
    OrderSetting selectByTenantCode(@Param("tenantCode") String tenantCode);

    @Insert("""
            INSERT INTO order_setting (
                tenant_code, stale_warning_days,
                sample_room_stale_warning_days, bulk_stale_warning_days,
                replenishment_stale_warning_days, drawing_budget_stale_warning_days,
                create_time, update_time
            )
            VALUES (
                #{tenantCode}, #{days},
                #{sampleRoomDays}, #{bulkDays}, #{replenishmentDays}, #{drawingBudgetDays},
                NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                stale_warning_days = VALUES(stale_warning_days),
                sample_room_stale_warning_days = VALUES(sample_room_stale_warning_days),
                bulk_stale_warning_days = VALUES(bulk_stale_warning_days),
                replenishment_stale_warning_days = VALUES(replenishment_stale_warning_days),
                drawing_budget_stale_warning_days = VALUES(drawing_budget_stale_warning_days),
                update_time = NOW()
            """)
    int upsertStaleWarningDays(@Param("tenantCode") String tenantCode,
                               @Param("days") Integer days,
                               @Param("sampleRoomDays") Integer sampleRoomDays,
                               @Param("bulkDays") Integer bulkDays,
                               @Param("replenishmentDays") Integer replenishmentDays,
                               @Param("drawingBudgetDays") Integer drawingBudgetDays);
}
