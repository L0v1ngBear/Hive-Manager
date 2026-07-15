package my.hive.domain.price.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
/**
 * PriceDetailVO 属于管理端后端价格模块，定义出参结构。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PriceDetailVO extends PriceSkuVO {

    private List<TierPriceVO> tierPrices = new ArrayList<>();

    private List<CustomerOverrideVO> overrides = new ArrayList<>();

    private List<PriceChangeLogVO> logs = new ArrayList<>();
}
