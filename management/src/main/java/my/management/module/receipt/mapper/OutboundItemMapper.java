package my.management.module.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.receipt.model.entity.OutboundItem;
import org.apache.ibatis.annotations.Mapper;
/**
 * OutboundItemMapper 属于管理端后端打印回执模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface OutboundItemMapper extends BaseMapper<OutboundItem> {
}
