package my.hive.infrastructure.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class UiReleaseEvents {
    private final String buildId;
    private final Set<SseEmitter> clients = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "ui-release-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean ready;

    public UiReleaseEvents(@Value("${hive.ui-release.version-file:}") String versionFile,
                           ObjectMapper objectMapper) {
        if (versionFile.isBlank()) {
            buildId = ""; // Local development without a production frontend has nothing to announce.
            return;
        }
        try {
            String value = objectMapper.readTree(Files.readString(Path.of(versionFile)))
                    .path("buildId").asText();
            if (!value.matches("[a-zA-Z0-9._-]{1,100}")) {
                throw new IllegalArgumentException("Invalid frontend build identifier");
            }
            buildId = value;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read frontend release metadata", exception);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        ready = true;
        clients.forEach(this::announce);
        // SSE comments keep proxies alive and detect closed clients; they do not check versions.
        heartbeat.scheduleWithFixedDelay(() -> clients.forEach(client ->
                send(client, SseEmitter.event().comment("keep-alive"))), 25, 25, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        SseEmitter client = new SseEmitter(0L);
        clients.add(client);
        client.onCompletion(() -> clients.remove(client));
        client.onTimeout(() -> clients.remove(client));
        client.onError(error -> clients.remove(client));
        send(client, SseEmitter.event().reconnectTime(15_000).comment("connected"));
        if (ready) announce(client);
        return client;
    }

    private void announce(SseEmitter client) {
        if (!buildId.isBlank()) {
            send(client, SseEmitter.event().name("release").id(buildId).data(Map.of("buildId", buildId)));
        }
    }

    private void send(SseEmitter client, SseEmitter.SseEventBuilder event) {
        try {
            client.send(event);
        } catch (IOException | IllegalStateException exception) {
            clients.remove(client);
            client.complete();
        }
    }

    @PreDestroy
    public void close() {
        heartbeat.shutdownNow();
        clients.forEach(SseEmitter::complete);
        clients.clear();
    }
}
