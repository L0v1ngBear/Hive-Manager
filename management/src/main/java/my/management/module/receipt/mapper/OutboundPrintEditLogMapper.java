package my.management.module.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.receipt.model.entity.OutboundPrintEditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库打印修正日志数据访问。
 */
@Mapper
public interface OutboundPrintEditLogMapper extends BaseMapper<OutboundPrintEditLog> {
}
