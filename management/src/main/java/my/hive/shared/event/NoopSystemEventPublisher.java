package my.hive.shared.event;

public class NoopSystemEventPublisher implements SystemEventPublisher {

    @Override
    public void publish(SystemEvent event) {
        // disabled intentionally
    }
}
