package my.hive.domain.aftersales.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sales_part")
public class AfterSalesPart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String partCode;
    private String partName;
    private String category;
    private String modelSpec;
    private BigDecimal unitPrice;
    private String unit;
    private Integer stockQty;
    private Integer reservedQty;
    private Integer safetyStock;
    private String photoUrl;
    private String remark;
    private Integer status;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
