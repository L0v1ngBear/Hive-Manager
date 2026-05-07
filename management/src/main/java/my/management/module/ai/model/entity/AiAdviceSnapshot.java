package my.management.module.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_advice_snapshot")
public class AiAdviceSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String snapshotJson;

    private Integer adviceCount;

    private String status;

    private String errorMessage;

    private LocalDateTime generatedAt;

    private LocalDateTime lastAttemptTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
