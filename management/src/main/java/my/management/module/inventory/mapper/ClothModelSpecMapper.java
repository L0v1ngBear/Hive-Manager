package my.management.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.inventory.model.entity.ClothModelSpec;
import my.management.module.inventory.model.vo.InventoryModelOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 型号规格字典数据访问层。
 */
@Mapper
public interface ClothModelSpecMapper extends BaseMapper<ClothModelSpec> {

    @Select({
            "SELECT model_code AS modelCode, spec ",
            "FROM cloth_model_spec ",
            "WHERE tenant_code = #{tenantCode} ",
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR model_code LIKE CONCAT('%', #{keyword}, '%')) ",
            "ORDER BY id DESC ",
            "LIMIT #{limit}"
    })
    List<InventoryModelOptionVO> search(@Param("tenantCode") String tenantCode,
                                        @Param("keyword") String keyword,
                                        @Param("limit") Integer limit);
}
