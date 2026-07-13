package my.management.module.sys.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
/**
 * SysPermissionTreeVO 属于管理端后端系统模块，定义出参结构。
 */
@Data
public class SysPermissionTreeVO {

    private Long id;

    private Long value;

    private String label;

    private Long parentId;

    private String permName;

    private String permCode;

    private String moduleCode;

    private Integer permType;

    private Integer assignable;

    private Integer status;

    private Integer sort;

    private List<SysPermissionTreeVO> children = new ArrayList<>();
}
