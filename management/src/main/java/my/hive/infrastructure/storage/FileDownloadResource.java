package my.hive.infrastructure.storage;

import org.springframework.core.io.Resource;

public record FileDownloadResource(Resource resource, String filename, String contentType) {
}
