package my.hive.api.storage;

import my.hive.infrastructure.storage.OssStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;

/**
 * Exposes only the explicitly public tenant-logo object class.
 */
@RestController
@RequestMapping("/storage/public")
public class PublicStorageController {

    @jakarta.annotation.Resource
    private OssStorageService ossStorageService;

    @GetMapping("/tenant-logo/{reference}")
    public ResponseEntity<Resource> tenantLogo(@PathVariable String reference) {
        Resource resource = ossStorageService.loadPublicTenantLogo(reference);
        return ResponseEntity.ok()
                .contentType(resolveImageType(resource.getFilename()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(resource);
    }

    private MediaType resolveImageType(String filename) {
        if (!StringUtils.hasText(filename)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String normalized = filename.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (normalized.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
