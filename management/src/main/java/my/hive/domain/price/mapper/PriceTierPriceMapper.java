package my.hive.domain.price.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.price.model.entity.PriceTierPrice;
import org.apache.ibatis.annotations.Mapper;
/**
 * PriceTierPriceMapper 属于管理端后端价格模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface PriceTierPriceMapper extends BaseMapper<PriceTierPrice> {
}
