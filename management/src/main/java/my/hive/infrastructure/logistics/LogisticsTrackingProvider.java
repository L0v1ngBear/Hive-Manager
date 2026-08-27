package my.hive.infrastructure.logistics;

import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;

public interface LogisticsTrackingProvider {

    String providerCode();

    /**
     * Whether the provider can identify a carrier from the waybill when no
     * company code is available. Legacy providers keep requiring a code.
     */
    default boolean supportsCompanyCodeAutoRecognition() {
        return false;
    }

    OrderLogisticsTrackingVO query(LogisticsTrackingQuery query);
}
