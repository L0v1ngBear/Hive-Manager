package my.management.module.tenant.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.tenant.model.entity.TenantUsageMeter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// TENANT-GUARDED: this mapper is used by platform license code and every query/update path
// is constrained by TenantUsageMeter::getTenantCode before BaseMapper is called.
@InterceptorIgnore(tenantLine = "true")
public interface TenantUsageMeterMapper extends BaseMapper<TenantUsageMeter> {
}
