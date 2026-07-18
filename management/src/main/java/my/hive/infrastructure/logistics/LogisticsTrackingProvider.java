package my.hive.infrastructure.logistics;

import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;

public interface LogisticsTrackingProvider {

    String providerCode();

    OrderLogisticsTrackingVO query(LogisticsTrackingQuery query);
}
