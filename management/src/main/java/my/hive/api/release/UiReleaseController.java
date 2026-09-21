package my.hive.api.release;

import my.hive.infrastructure.release.UiReleaseEvents;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class UiReleaseController {
    private final UiReleaseEvents events;

    public UiReleaseController(UiReleaseEvents events) {
        this.events = events;
    }

    // Public build metadata only. Business data and tenant sessions never enter this stream.
    @GetMapping(value = "/release/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Accel-Buffering", "no").body(events.subscribe());
    }
}
