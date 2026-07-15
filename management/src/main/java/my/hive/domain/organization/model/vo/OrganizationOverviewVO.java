package my.hive.domain.organization.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构页面总览出参。
 */
@Data
public class OrganizationOverviewVO {

    private OrganizationStatsVO stats = new OrganizationStatsVO();

    private List<OrganizationDepartmentVO> departments = new ArrayList<>();
}
