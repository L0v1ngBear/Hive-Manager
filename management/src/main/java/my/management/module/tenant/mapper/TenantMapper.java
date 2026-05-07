package my.management.module.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.tenant.model.entity.Tenant;
import org.apache.ibatis.annotations.Select;

public interface TenantMapper extends BaseMapper<Tenant> {

    @Select("SELECT * FROM tenant WHERE tenant_code = #{tenantCode} AND deleted = 0 LIMIT 1")
    Tenant selectByTenantCode(String tenantCode);
}
