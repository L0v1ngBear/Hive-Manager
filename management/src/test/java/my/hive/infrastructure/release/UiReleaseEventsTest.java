package my.hive.infrastructure.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.hive.api.release.UiReleaseController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UiReleaseEventsTest {
    @TempDir Path directory;

    @Test
    void publishesOnlyTheReadyBuildAndReplaysItOnReconnect() throws Exception {
        Path version = directory.resolve("version.json");
        Files.writeString(version, "{\"buildId\":\"build-123\"}");
        UiReleaseEvents events = new UiReleaseEvents(version.toString(), new ObjectMapper());
        try {
            var mvc = MockMvcBuilders.standaloneSetup(new UiReleaseController(events)).build();
            var beforeReady = mvc.perform(get("/release/events"))
                    .andExpect(status().isOk()).andExpect(request().asyncStarted())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("X-Accel-Buffering", "no")).andReturn();
            assertThat(beforeReady.getResponse().getContentAsString()).doesNotContain("event:release");
            events.onReady();
            assertThat(beforeReady.getResponse().getContentAsString())
                    .contains("event:release", "\"buildId\":\"build-123\"", "retry:15000");
            // Deployments recreate the backend. A partially uploaded file must not change this instance's announcement.
            Files.writeString(version, "{\"buildId\":\"not-active-yet\"}");
            var reconnect = mvc.perform(get("/release/events")).andExpect(request().asyncStarted()).andReturn();
            assertThat(reconnect.getResponse().getContentAsString()).contains("build-123").doesNotContain("not-active-yet");
        } finally { events.close(); }
    }

    @Test
    void refusesMissingOrInvalidConfiguredMetadata() throws Exception {
        assertThatThrownBy(() -> new UiReleaseEvents(directory.resolve("missing.json").toString(), new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class);
        Path version = directory.resolve("version.json");
        Files.writeString(version, "{\"buildId\":\"<bad>\"}");
        assertThatThrownBy(() -> new UiReleaseEvents(version.toString(), new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unconfiguredDevelopmentDoesNotAnnounceAFakeRelease() throws Exception {
        UiReleaseEvents events = new UiReleaseEvents("", new ObjectMapper());
        try {
            events.onReady();
            var result = MockMvcBuilders.standaloneSetup(new UiReleaseController(events)).build()
                    .perform(get("/release/events")).andExpect(request().asyncStarted()).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("event:release");
        } finally { events.close(); }
    }
}
