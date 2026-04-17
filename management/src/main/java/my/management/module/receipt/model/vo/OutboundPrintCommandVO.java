package my.management.module.receipt.model.vo;

import lombok.Data;
/**
 * OutboundPrintCommandVO 属于管理端后端打印回执模块，定义出参结构。
 */
@Data
public class OutboundPrintCommandVO {

    private String orderNo;

    private String fileName;

    private String driverType;

    private String contentType;

    private String charset;

    private String base64Content;
}
