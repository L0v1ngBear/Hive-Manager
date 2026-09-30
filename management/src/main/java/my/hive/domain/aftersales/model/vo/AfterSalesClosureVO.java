package my.hive.domain.aftersales.model.vo;

import java.time.LocalDateTime;

public record AfterSalesClosureVO(String resolution, LocalDateTime closedTime, String operatorName) {}
