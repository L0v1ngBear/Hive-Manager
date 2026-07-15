package my.hive.api.system;

import my.hive.shared.dto.Result;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final BuildProperties buildProperties;

    public HealthController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("status", "UP");
        details.put("application", buildProperties.getName());
        details.put("version", buildProperties.getVersion());
        return Result.success(details);
    }
}
