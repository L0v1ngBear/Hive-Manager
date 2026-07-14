package my.hive.domain.inventory.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 图片识别入库结果。当前先返回可编辑候选，最终入库仍走库存入库主链路。
 */
@Data
public class InventoryImageRecognitionVO {

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String status;

    private String message;

    private BigDecimal confidence;

    private List<Candidate> candidates;

    @Data
    public static class Candidate {

        private String barcode;

        private String modelCode;

        private BigDecimal spec;

        private BigDecimal meters;

        private BigDecimal confidence;

        private String sourceText;
    }
}