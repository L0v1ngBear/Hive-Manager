package my.hive.domain.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.tenant.model.entity.TenantFieldConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantFieldConfigMapper extends BaseMapper<TenantFieldConfig> {
}
