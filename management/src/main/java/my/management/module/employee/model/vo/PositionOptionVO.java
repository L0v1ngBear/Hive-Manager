package my.management.module.employee.model.vo;

import lombok.Data;

@Data
public class PositionOptionVO {

    private Long id;
    private String name;
    private String code;
    private Long departmentId;
}
