package my.hive.domain.print.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.print.receipt.model.entity.OutboundPrintEditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库打印修正日志数据访问。
 */
@Mapper
public interface OutboundPrintEditLogMapper extends BaseMapper<OutboundPrintEditLog> {
}
