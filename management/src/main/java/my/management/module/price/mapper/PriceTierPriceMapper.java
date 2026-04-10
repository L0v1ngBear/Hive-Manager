package my.management.module.price.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.price.model.entity.PriceTierPrice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PriceTierPriceMapper extends BaseMapper<PriceTierPrice> {
}