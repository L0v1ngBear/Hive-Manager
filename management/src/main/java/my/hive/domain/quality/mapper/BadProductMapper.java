package my.hive.domain.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.quality.model.entity.BadProductRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质量记录数据访问层。
 */
@Mapper
public interface BadProductMapper extends BaseMapper<BadProductRecord> {
}