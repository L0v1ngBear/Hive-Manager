package my.hive.domain.installation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InstallationTaskVO {

    private Long id;

    private String orderId;

    private String orderStatus;

    private String installationStatus;

    private String customerName;

    private String customerPhone;

    private String projectName;

    private String brandName;

    private String orderCategory;

    private String goodsDesc;

    private Integer totalQuantity;

    private String informationChannel;

    private String expressCompany;

    private String expressNo;

    private Integer isInvoice;

    private String creator;

    private String remark;

    private String orderAttachmentName;

    private String orderAttachmentUrl;

    private Long orderAttachmentSize;

    private String constructionPersonnel;

    private String constructionPhone;

    private String constructionRemark;

    private String specialExceptionNote;

    private String attachmentName;

    private String attachmentUrl;

    private Long attachmentSize;

    private LocalDateTime orderCompletedTime;

    private LocalDateTime acceptedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}