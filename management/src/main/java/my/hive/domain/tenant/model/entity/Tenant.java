package my.hive.domain.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * Tenant 属于管理端后端租户模块，定义持久化实体结构，用于表字段映射。
 */
@Data
@TableName("tenant")
public class Tenant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String tenantName;

    private String logoUrl;

    private Integer tenantType;

    private String contactPerson;

    private String contactPhone;

    private String password;

    private Integer status;

    private String packageCode;

    private String packageName;

    private String subscriptionStatus;

    private LocalDateTime subscriptionStartTime;

    private LocalDateTime subscriptionEndTime;

    private Integer maxUsers;

    private Integer maxStorageMb;

    private String featureFlags;

    private Long creator;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
