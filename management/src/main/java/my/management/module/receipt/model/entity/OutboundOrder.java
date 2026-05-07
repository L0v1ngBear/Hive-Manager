package my.management.module.receipt.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * OutboundOrder 属于管理端后端打印回执模块，定义持久化实体结构，用于表字段映射。
 */
@Data
@TableName("outbound_order")
public class OutboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String orderNo;

    private String customerName;

    private String projectName;

    private LocalDate printDate;

    private String logisticsCompany;

    private String logisticsNo;

    private String printOperatorName;

    private Integer printEditCount;

    private Integer orderStatus;

    private Integer printStatus;

    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
