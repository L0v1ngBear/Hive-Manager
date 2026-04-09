package my.management.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.employee.model.entity.EmployeeExt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeExtMapper extends BaseMapper<EmployeeExt> {
}
