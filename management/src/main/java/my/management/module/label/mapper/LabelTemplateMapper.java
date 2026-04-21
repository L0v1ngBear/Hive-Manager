package my.management.module.label.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.label.model.entity.LabelTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签模板数据访问层，对应 label_template 表。
 */
@Mapper
public interface LabelTemplateMapper extends BaseMapper<LabelTemplate> {
}
