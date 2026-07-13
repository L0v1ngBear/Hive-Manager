package my.management.module.order.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售订单保存请求，同时用于管理端新建和完整编辑。
 */
@Data
public class SalesOrderSaveRequest {

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String customerPhone;

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    private String brandName;

    private String orderCategory;

    private String informationChannel;

    /**
     * 业务录单时间。允许成熟客户补录历史订单时手动指定，不传时使用服务端当前时间。
     */
    private String createTime;

    private String expressCompany;

    private String expressNo;

    /**
     * 是否开票：0-否，1-是。管理端新建时默认未开票。
     */
    private Integer isInvoice;

    private String remark;

    /**
     * 销售订单附件名称，通常用于保存合同、客户需求或沟通截图的原始文件名。
     */
    private String attachmentName;

    /**
     * 销售订单附件访问地址，由附件上传接口返回。
     */
    private String attachmentUrl;

    /**
     * 销售订单附件大小，单位字节，便于前端展示和后续清理策略判断。
     */
    private Long attachmentSize;

    private String status;

    private Integer createProductionOrder;

    private List<Long> auditorIds;

    @Valid
    private List<ItemDTO> items;

    /**
     * 明细项结构和小程序下单页保持一致，减少两端心智差异。
     */
    @Data
    public static class ItemDTO {

        private Long id;

        private String modelCode;

        private BigDecimal quantity;

        private String weight;

        private Float spec;
    }
}
