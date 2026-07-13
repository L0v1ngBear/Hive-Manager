package my.management.module.receipt.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记录出库单打印前人工修正的前后快照，便于纸质单据和系统记录追溯。
 */
@Data
@TableName("outbound_print_edit_log")
public class OutboundPrintEditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private Long orderId;

    private String orderNoBefore;

    private String orderNoAfter;

    private Long operatorUserId;

    private String beforeJson;

    private String afterJson;

    private String editReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
