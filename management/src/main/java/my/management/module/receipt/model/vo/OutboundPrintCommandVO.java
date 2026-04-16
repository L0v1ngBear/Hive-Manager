package my.management.module.receipt.model.vo;

import lombok.Data;

@Data
public class OutboundPrintCommandVO {

    private String orderNo;

    private String fileName;

    private String driverType;

    private String contentType;

    private String charset;

    private String base64Content;
}
