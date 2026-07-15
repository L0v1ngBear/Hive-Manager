package my.hive.domain.attendance.model.dto;

import lombok.Data;

@Data
public class AttendanceLocationSaveRequest {

    private Long id;

    private String locationName;

    private Double latitude;

    private Double longitude;

    private String address;

    private Double radius;
}
