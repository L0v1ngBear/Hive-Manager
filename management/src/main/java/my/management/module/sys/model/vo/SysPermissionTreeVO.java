package my.management.module.sys.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SysPermissionTreeVO {

    private Long id;

    private Long value;

    private String label;

    private Long parentId;

    private String permName;

    private String permCode;

    private Integer sort;

    private List<SysPermissionTreeVO> children = new ArrayList<>();
}
