package my.hive.domain.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.tenant.model.entity.Tenant;
import org.apache.ibatis.annotations.Select;

public interface TenantMapper extends BaseMapper<Tenant> {

    @Select("""
            SELECT id, tenant_code, tenant_name, tenant_type, contact_person, contact_phone,
                   password, status, package_code, package_name, subscription_status,
                   subscription_start_time, subscription_end_time, max_users,
                   max_storage_mb, feature_flags, creator,
                   create_time, update_time, deleted
            FROM tenant
            WHERE tenant_code = #{tenantCode}
              AND deleted = 0
            LIMIT 1
            """)
    Tenant selectByTenantCode(String tenantCode);
}
