package my.management.module.approval.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_leave")
public class UserLeave {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String leaveCode;

    private String tenantCode;

    private Long applyUserId;

    private Integer leaveType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String reason;

    private Integer status;

    private String auditComment;

    private Long auditorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
