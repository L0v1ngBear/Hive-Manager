package my.hive.domain.permission.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_permission")
public class SysUserPermission {

    public static final String EFFECT_GRANT = "GRANT";
    public static final String EFFECT_DENY = "DENY";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long userId;

    private Long permissionId;

    private String effect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
