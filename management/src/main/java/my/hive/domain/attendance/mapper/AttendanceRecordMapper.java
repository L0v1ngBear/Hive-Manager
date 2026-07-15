package my.hive.domain.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.attendance.model.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤记录写入 Mapper。
 * 目前主要用于请假审批通过后，将请假结果同步沉淀到考勤记录表。
 */
@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {
}
