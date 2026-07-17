package my.hive.domain.order.model.vo;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderLogisticsTrackingVO {

    private String company;
    private String companyCode;
    private String trackingNo;
    private String state;
    private String stateLabel;
    private String latestContext;
    private String latestTime;
    private boolean cached;
    private Instant queriedAt;
    private Instant cacheExpiresAt;
    private List<TraceVO> traces = new ArrayList<>();

    @Data
    public static class TraceVO {
        private String context;
        private String time;
        private String status;
        private String statusCode;
        private String location;
    }
}
