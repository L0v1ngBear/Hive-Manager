package my.management.module.price.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.price.model.entity.PriceChangeLog;
import org.apache.ibatis.annotations.Mapper;
/**
 * PriceChangeLogMapper 属于管理端后端价格模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface PriceChangeLogMapper extends BaseMapper<PriceChangeLog> {
}
