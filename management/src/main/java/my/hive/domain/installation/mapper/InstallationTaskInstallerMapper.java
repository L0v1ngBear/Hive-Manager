package my.hive.domain.installation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.installation.model.entity.InstallationTaskInstaller;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InstallationTaskInstallerMapper extends BaseMapper<InstallationTaskInstaller> {
}
