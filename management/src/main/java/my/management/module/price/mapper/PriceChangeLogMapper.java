package my.management.module.price.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.price.model.entity.PriceChangeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PriceChangeLogMapper extends BaseMapper<PriceChangeLog> {
}