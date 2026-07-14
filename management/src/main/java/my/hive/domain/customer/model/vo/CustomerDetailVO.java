package my.hive.domain.customer.model.vo;

import lombok.Data;
import my.hive.domain.customer.model.entity.CustomerContact;
import my.hive.domain.customer.model.entity.CustomerProject;

import java.util.List;
/**
 * CustomerDetailVO 属于管理端后端客户模块，定义出参结构。
 */
@Data
public class CustomerDetailVO {
    private Long id;
    private String customerName;
    private Integer customerType;
    private String constructionArea;
    private List<CustomerContact> contacts;
    private List<CustomerProject> projects;
}
