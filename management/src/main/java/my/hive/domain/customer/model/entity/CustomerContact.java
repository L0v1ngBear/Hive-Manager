package my.hive.domain.customer.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
/**
 * CustomerContact 属于管理端后端客户模块，定义持久化实体结构，用于表字段映射。
 */
@TableName
@Data
public class CustomerContact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long customerId;

    private String contactName;

    private String contactPhone;
}
