package my.management.module.approval.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("finance_approval")
public class FinanceApproval {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String approvalCode;

    private String tenantCode;

    private Long applyUserId;

    private String category;

    private BigDecimal amount;

    private String reason;

    private String attachmentUrl;

    private Integer status;

    private Long auditorId;

    private String auditComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
