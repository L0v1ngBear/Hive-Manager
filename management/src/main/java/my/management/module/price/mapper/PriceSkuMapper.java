package my.management.module.price.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.management.module.price.model.entity.PriceSku;
import my.management.module.price.model.vo.PriceStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
/**
 * PriceSkuMapper 属于管理端后端价格模块，是数据访问类，负责与数据库交互。
 */
@Mapper
public interface PriceSkuMapper extends BaseMapper<PriceSku> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COALESCE(base_price, 0) FROM price_sku WHERE tenant_code = #{tenantCode} AND model_code = #{modelCode} AND status = 1 AND is_deleted = 0 ORDER BY effective_date DESC, id DESC LIMIT 1")
    BigDecimal getActivePrice(@Param("tenantCode") String tenantCode, @Param("modelCode") String modelCode);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(1) AS skuCount, COALESCE(AVG(base_price), 0) AS averagePrice, SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS pendingCount FROM price_sku WHERE tenant_code = #{tenantCode} AND is_deleted = 0")
    PriceStatsVO selectStats(@Param("tenantCode") String tenantCode);
}
