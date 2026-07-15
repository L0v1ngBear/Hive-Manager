package my.hive.domain.employee.model.vo;

import lombok.Data;
/**
 * PositionOptionVO 属于管理端后端员工模块，定义出参结构。
 */
@Data
public class PositionOptionVO {

    private Long id;
    private String name;
    private String code;
    private Long departmentId;
}
