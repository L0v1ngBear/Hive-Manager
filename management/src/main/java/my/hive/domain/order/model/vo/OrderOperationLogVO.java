package my.hive.domain.order.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderOperationLogVO {

    private Long id;
    private String action;
    private String description;
    private String operatorName;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime createTime;
}
