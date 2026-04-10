package my.management.module.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.receipt.model.entity.OutboundItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboundItemMapper extends BaseMapper<OutboundItem> {
}