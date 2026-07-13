package my.management.module.price.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.price.model.entity.PriceCustomerOverride;
import org.apache.ibatis.annotations.Mapper;
/**
 * PriceCustomerOverrideMapper 属于管理端后端价格模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface PriceCustomerOverrideMapper extends BaseMapper<PriceCustomerOverride> {
}
