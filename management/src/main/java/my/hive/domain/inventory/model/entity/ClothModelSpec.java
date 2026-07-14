package my.hive.domain.inventory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 型号规格字典实体，用于入库时快速选择常用型号和规格。
 */
@Data
@TableName("cloth_model_spec")
public class ClothModelSpec {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("model_code")
    private String modelCode;

    private BigDecimal spec;
}