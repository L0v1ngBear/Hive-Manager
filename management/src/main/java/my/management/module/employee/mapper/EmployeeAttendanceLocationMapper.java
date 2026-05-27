package my.management.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.employee.model.entity.EmployeeAttendanceLocation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeAttendanceLocationMapper extends BaseMapper<EmployeeAttendanceLocation> {
}
