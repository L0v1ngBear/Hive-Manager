package my.management.module.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.approval.model.entity.FinanceApproval;
import org.apache.ibatis.annotations.Mapper;
/**
 * FinanceApprovalMapper 属于管理端后端审批模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface FinanceApprovalMapper extends BaseMapper<FinanceApproval> {
}
