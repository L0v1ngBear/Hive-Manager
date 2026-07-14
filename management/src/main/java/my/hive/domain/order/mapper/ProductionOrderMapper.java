package my.hive.domain.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.order.model.entity.ProductionOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产订单数据访问层。
 */
@Mapper
public interface ProductionOrderMapper extends BaseMapper<ProductionOrder> {
}
