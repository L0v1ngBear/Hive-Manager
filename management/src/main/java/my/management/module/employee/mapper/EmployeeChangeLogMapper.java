package my.management.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.employee.model.entity.EmployeeChangeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeChangeLogMapper extends BaseMapper<EmployeeChangeLog> {
}
