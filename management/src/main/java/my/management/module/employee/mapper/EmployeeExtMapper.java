package my.management.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.employee.model.entity.EmployeeExt;
import org.apache.ibatis.annotations.Mapper;
/**
 * EmployeeExtMapper 属于管理端后端员工模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface EmployeeExtMapper extends BaseMapper<EmployeeExt> {
}
