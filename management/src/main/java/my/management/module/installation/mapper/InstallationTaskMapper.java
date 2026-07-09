package my.management.module.installation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.installation.model.entity.InstallationTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InstallationTaskMapper extends BaseMapper<InstallationTask> {
}
