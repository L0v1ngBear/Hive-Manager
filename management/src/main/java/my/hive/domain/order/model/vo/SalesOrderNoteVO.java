package my.hive.domain.order.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalesOrderNoteVO {
    @JsonSerialize(using = ToStringSerializer.class)
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
