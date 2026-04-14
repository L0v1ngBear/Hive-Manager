package my.management.module.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.approval.model.entity.UserLeave;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LeaveMapper extends BaseMapper<UserLeave> {
}
