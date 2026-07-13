package my.management.module.equipment.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentSaveRequest {

    private Long id;

    @Size(max = 80, message = "设备编码最长80个字符")
    private String equipmentCode;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 120, message = "设备名称最长120个字符")
    private String equipmentName;

    @Size(max = 80, message = "设备类型最长80个字符")
    private String equipmentType;

    @Size(max = 160, message = "设备位置最长160个字符")
    private String location;

    @Size(max = 80, message = "负责人最长80个字符")
    private String responsiblePerson;

    @Min(value = 1, message = "巡检周期不能小于1天")
    @Max(value = 3650, message = "巡检周期不能超过3650天")
    private Integer inspectionCycleDays;

    @Size(max = 30, message = "状态最长30个字符")
    private String status;

    @Size(max = 500, message = "备注最长500个字符")
    private String remark;
}
