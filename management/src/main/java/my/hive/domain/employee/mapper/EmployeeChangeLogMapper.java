package my.hive.domain.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.employee.model.entity.EmployeeChangeLog;
import org.apache.ibatis.annotations.Mapper;
/**
 * EmployeeChangeLogMapper 属于管理端后端员工模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface EmployeeChangeLogMapper extends BaseMapper<EmployeeChangeLog> {
}
