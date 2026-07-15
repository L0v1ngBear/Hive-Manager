package my.hive.domain.attendance.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendancePunchRequest {

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double userLat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double userLng;
}
