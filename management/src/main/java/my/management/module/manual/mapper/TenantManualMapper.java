package my.management.module.manual.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.manual.model.entity.TenantManual;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@InterceptorIgnore(tenantLine = "true")
public interface TenantManualMapper extends BaseMapper<TenantManual> {

    @Select("""
            SELECT id, tenant_code, content, updater_id, create_time, update_time
            FROM tenant_manual
            WHERE tenant_code = #{tenantCode}
            LIMIT 1
            """)
    TenantManual selectByTenantCode(@Param("tenantCode") String tenantCode);

    @Insert("""
            INSERT INTO tenant_manual (tenant_code, content, updater_id, create_time, update_time)
            VALUES (#{tenantCode}, #{content}, #{updaterId}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                content = VALUES(content),
                updater_id = VALUES(updater_id),
                update_time = NOW()
            """)
    int upsert(@Param("tenantCode") String tenantCode,
               @Param("content") String content,
               @Param("updaterId") Long updaterId);
}
