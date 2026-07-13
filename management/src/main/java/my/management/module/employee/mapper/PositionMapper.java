package my.management.module.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.employee.model.entity.Position;
import org.apache.ibatis.annotations.Mapper;
/**
 * PositionMapper 属于管理端后端员工模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface PositionMapper extends BaseMapper<Position> {
}
