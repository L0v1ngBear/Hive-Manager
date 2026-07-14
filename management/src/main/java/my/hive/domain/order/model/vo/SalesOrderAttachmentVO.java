package my.hive.domain.order.model.vo;

import lombok.Data;

/**
 * 销售订单附件上传结果。
 * 前端先调用上传接口拿到该对象，再把附件信息随销售订单一起保存。
 */
@Data
public class SalesOrderAttachmentVO {

    /**
     * 用户上传时的原始文件名。
     */
    private String fileName;

    /**
     * 可访问的相对地址，默认走管理端后端的静态资源映射。
     */
    private String fileUrl;

    /**
     * 文件大小，单位字节。
     */
    private Long fileSize;
}
