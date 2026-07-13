package my.management.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.order.model.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单主表数据访问层。
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
}
