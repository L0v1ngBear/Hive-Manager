package my.management.module.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.approval.model.entity.ResignationApproval;
import org.apache.ibatis.annotations.Mapper;

/**
 * 离职审批数据访问。
 */
@Mapper
public interface ResignationApprovalMapper extends BaseMapper<ResignationApproval> {
}
