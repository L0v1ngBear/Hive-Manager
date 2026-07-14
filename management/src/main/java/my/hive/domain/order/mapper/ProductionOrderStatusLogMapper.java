package my.hive.domain.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.order.model.entity.ProductionOrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产订单状态日志数据访问层。
 */
@Mapper
public interface ProductionOrderStatusLogMapper extends BaseMapper<ProductionOrderStatusLog> {
}
