package my.hive.shared.log;

/**
 * 操作日志收集器扩展点。
 * 默认实现会写结构化应用日志；如果需要落库或接入 ELK/ClickHouse，只需在业务后端实现该接口。
 */
public interface OperationLogCollector {

    void collect(OperationLogEvent event);
}
