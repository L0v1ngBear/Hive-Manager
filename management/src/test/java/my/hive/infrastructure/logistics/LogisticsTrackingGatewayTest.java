package my.hive.infrastructure.logistics;

import my.hive.domain.order.model.vo.OrderLogisticsTrackingVO;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogisticsTrackingGatewayTest {

    @Test
    void selectsTheConfiguredProviderCaseInsensitively() {
        RecordingProvider apispace = new RecordingProvider("apispace");
        RecordingProvider stub = new RecordingProvider("stub");
        LogisticsTrackingQuery query = new LogisticsTrackingQuery("sf", "SF123456", "1234");

        LogisticsTrackingGateway gateway = new LogisticsTrackingGateway("  APISPACE  ", List.of(apispace, stub));

        assertThat(gateway.providerCode()).isEqualTo("apispace");
        assertThat(gateway.query(query)).isSameAs(apispace.result);
        assertThat(apispace.receivedQuery).isEqualTo(query);
        assertThat(stub.receivedQuery).isNull();
    }

    @Test
    void rejectsAnUnknownProviderWithoutCallingAnyProvider() {
        RecordingProvider apispace = new RecordingProvider("apispace");
        RecordingProvider stub = new RecordingProvider("stub");
        LogisticsTrackingGateway gateway = new LogisticsTrackingGateway("missing", List.of(apispace, stub));

        assertThatThrownBy(() -> gateway.query(new LogisticsTrackingQuery("sf", "SF123456", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);
        assertThat(apispace.receivedQuery).isNull();
        assertThat(stub.receivedQuery).isNull();
    }

    @Test
    void rejectsDuplicateProviderCodes() {
        assertThatThrownBy(() -> new LogisticsTrackingGateway("apispace", List.of(
                new RecordingProvider("apispace"), new RecordingProvider("APISPACE"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class RecordingProvider implements LogisticsTrackingProvider {

        private final String code;
        private final OrderLogisticsTrackingVO result = new OrderLogisticsTrackingVO();
        private LogisticsTrackingQuery receivedQuery;

        private RecordingProvider(String code) {
            this.code = code;
        }

        @Override
        public String providerCode() {
            return code;
        }

        @Override
        public OrderLogisticsTrackingVO query(LogisticsTrackingQuery query) {
            receivedQuery = query;
            return result;
        }
    }
}
