package my.management.module.tenant.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.tenant.model.entity.TenantUsageMeter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TenantUsageMeterMapper extends BaseMapper<TenantUsageMeter> {
}
