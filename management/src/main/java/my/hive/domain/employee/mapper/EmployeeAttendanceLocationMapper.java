package my.hive.domain.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.employee.model.entity.EmployeeAttendanceLocation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeAttendanceLocationMapper extends BaseMapper<EmployeeAttendanceLocation> {
}
