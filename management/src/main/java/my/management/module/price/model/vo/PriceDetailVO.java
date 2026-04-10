package my.management.module.price.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PriceDetailVO extends PriceSkuVO {

    private List<TierPriceVO> tierPrices = new ArrayList<>();

    private List<CustomerOverrideVO> overrides = new ArrayList<>();

    private List<PriceChangeLogVO> logs = new ArrayList<>();
}