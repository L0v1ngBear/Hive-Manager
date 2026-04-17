package my.management.module.sys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * SysUserRole 属于管理端后端系统模块，定义持久化实体结构，用于表字段映射。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tenantCode;

    private Long roleId;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
