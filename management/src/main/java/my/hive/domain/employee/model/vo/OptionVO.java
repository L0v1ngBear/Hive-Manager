package my.hive.domain.employee.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * OptionVO 属于管理端后端员工模块，定义出参结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionVO {

    private String label;
    private String value;
}
