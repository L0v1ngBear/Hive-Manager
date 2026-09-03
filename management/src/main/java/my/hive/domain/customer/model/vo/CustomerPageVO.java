package my.hive.domain.customer.model.vo;

import lombok.Data;
import my.hive.domain.customer.model.entity.CustomerContact;
import my.hive.domain.customer.model.entity.CustomerProject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
/**
 * CustomerPageVO 属于管理端后端客户模块，定义出参结构。
 */
@Data
public class CustomerPageVO {
    private Long id;
    private String customerName;
    private Integer customerType;
    private String constructionArea;
    private String customerAddress;
    private LocalDate openingDate;
    private String sourceType;
    private LocalDateTime importTime;
    private Long importUserId;
    private String importUserName;
    private Integer projectCount;
    private List<String> projectNames;
    private List<CustomerContact> contacts;
    private List<CustomerProject> projects;
}
