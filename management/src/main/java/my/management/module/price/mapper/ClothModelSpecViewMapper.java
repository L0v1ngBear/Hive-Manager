package my.management.module.price.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import my.management.module.price.model.vo.ModelSpecOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
/**
 * ClothModelSpecViewMapper 属于管理端后端价格模块，是数据访问类，负责与数据库交互。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ClothModelSpecViewMapper {

    @Select({
            "<script>",
            "SELECT model_code AS modelCode, MAX(spec) AS spec ",
            "FROM cloth_model_spec ",
            "WHERE tenant_code = #{tenantCode} ",
            "<if test='keyword != null and keyword != \"\"'>AND model_code LIKE CONCAT('%', #{keyword}, '%') </if>",
            "GROUP BY model_code ORDER BY model_code LIMIT #{limit}",
            "</script>"
    })
    List<ModelSpecOptionVO> searchModelSpec(@Param("tenantCode") String tenantCode, @Param("keyword") String keyword, @Param("limit") Integer limit);
}
