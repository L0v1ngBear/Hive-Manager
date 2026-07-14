package my.hive.domain.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.order.model.entity.SalesOrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单明细数据访问层。
 */
@Mapper
public interface SalesOrderDetailMapper extends BaseMapper<SalesOrderDetail> {
}
