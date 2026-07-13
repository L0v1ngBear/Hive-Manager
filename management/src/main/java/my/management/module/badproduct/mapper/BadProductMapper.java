package my.management.module.badproduct.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.badproduct.model.entity.BadProductRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质量记录数据访问层。
 */
@Mapper
public interface BadProductMapper extends BaseMapper<BadProductRecord> {
}
