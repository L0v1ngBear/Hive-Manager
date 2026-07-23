package my.hive.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Controls lossless/lossy processing performed before a file is handed to a
 * storage provider.  Keeping this before the provider router guarantees the
 * same behaviour for local disks and OSS.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.upload.image-compression")
public class MediaUploadProperties {

    /** Enable safe image resizing and JPEG recompression. */
    private boolean enabled = true;

    /** Do not spend CPU on small images where the saving is negligible. */
    private long minFileSizeKb = 250;

    /** Longest edge of a processed image. */
    private int maxDimension = 2560;

    /** Safety limit while decoding an image received from an untrusted client. */
    private long maxPixels = 40_000_000L;

    /** JPEG output quality, in the inclusive range 0.0 to 1.0. */
    private float jpegQuality = 0.82F;
}
