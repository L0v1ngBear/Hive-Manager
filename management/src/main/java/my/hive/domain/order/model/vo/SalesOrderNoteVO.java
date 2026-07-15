package my.hive.domain.order.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalesOrderNoteVO {
    private Long id;
    private String content;
    private Long creatorUserId;
    private String creatorName;
    private Long updaterUserId;
    private String updaterName;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
