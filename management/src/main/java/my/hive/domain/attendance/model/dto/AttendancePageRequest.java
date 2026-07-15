package my.hive.domain.attendance.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 考勤分页查询入参。
 */
@Data
public class AttendancePageRequest {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    /**
     * 员工姓名、手机号或工号关键字。
     */
    private String keyword;

    /**
     * 服务端根据 keyword 生成的手机号哈希，前端无需传入。
     */
    private String keywordPhoneHash;

    /**
     * 部门名称，前端选择“全部部门”时传空。
     */
    private String departmentName;

    /**
     * 查询日期，默认使用当天。
     */
    private LocalDate date;

    /**
     * 组合状态：normal/late/early/missing/leave/overtime。
     */
    private String status;
}
