package my.management.module.attendance.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考勤记录实体。
 * 管理端审批请假通过后也会写入该表，避免网页端审批和小程序考勤统计出现状态不一致。
 */
@Data
@TableName("attendance_record")
public class AttendanceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 每日考勤唯一编号，格式为 yyyyMMdd_用户ID。
     */
    private String punchId;

    private Long userId;

    private String tenantCode;

    private LocalTime signInTime;

    /**
     * 上班状态：0正常、1迟到、3缺勤、5请假、6缺卡。
     */
    private Integer signInStatus;

    private LocalTime signOutTime;

    /**
     * 下班状态：0正常、2早退、4加班、5请假、6缺卡。
     */
    private Integer signOutStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
