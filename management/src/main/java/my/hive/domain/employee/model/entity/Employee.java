package my.hive.domain.employee.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * Employee 属于管理端后端员工模块，定义持久化实体结构，用于表字段映射。
 */
@TableName("user")
@Data
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String name;

    private String loginName;

    private String phone;

    /**
     * 手机号不可逆哈希，用于登录和查重，不对前端返回。
     */
    private String phoneHash;

    /**
     * 手机号脱敏值，用于列表、详情和导出展示。
     */
    private String phoneMask;

    @TableField(select = false)
    private String password;

    private Integer mustChangePassword;

    private String departmentName;

    private String position;

    private Long managerId;

    private String managerName;

    private Integer status;

    private Integer attendanceRequired;

    private Integer roleLevel;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long permissionVersion;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long authVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
