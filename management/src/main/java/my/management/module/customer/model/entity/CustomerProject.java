package my.management.module.customer.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("customer_project")
@Data
public class CustomerProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long customerId;

    private String projectName;
}
