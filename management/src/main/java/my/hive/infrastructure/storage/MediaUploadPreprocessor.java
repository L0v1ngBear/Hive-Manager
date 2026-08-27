package my.hive.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import my.hive.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

/**
 * Processes only formats that the JDK can encode predictably. Videos and
 * unsupported image formats deliberately pass through unchanged: video
 * transcoding belongs in an asynchronous worker, never in an HTTP upload.
 */
@Slf4j
@Service
public class MediaUploadPreprocessor {

    private final MediaUploadProperties properties;

    public MediaUploadPreprocessor(MediaUploadProperties properties) {
        this.properties = properties;
    }

    public MultipartFile prepare(MultipartFile source) {
        if (source == null || source.isEmpty() || !properties.isEnabled() || !isProcessableImage(source)) {
            return source;
        }
        if (source.getSize() < Math.max(0, properties.getMinFileSizeKb()) * 1024L) {
            return source;
        }

        try (InputStream inputStream = source.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(inputStream)) {
            if (imageInput == null) {
                return source;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                return source;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (pixels <= 0 || pixels > Math.max(1, properties.getMaxPixels())) {
                    throw new BusinessException("图片像素过大，无法上传");
                }

                // Decode only after validating dimensions. ImageIO.read would
                // allocate the complete bitmap before this safety check.
                BufferedImage original = reader.read(0);
                if (original == null) {
                    return source;
                }

                BufferedImage scaled = resizeIfNeeded(original);
                String extension = extensionOf(source.getOriginalFilename());
                boolean jpeg = "jpg".equals(extension) || "jpeg".equals(extension);
                byte[] processed = jpeg ? writeJpeg(scaled) : writePng(scaled);

                // Keep the original if re-encoding has not actually saved space.
                if (processed.length >= source.getSize()) {
                    return source;
                }
                String contentType = jpeg ? "image/jpeg" : "image/png";
                String filename = jpeg ? withExtension(source.getOriginalFilename(), "jpg") : source.getOriginalFilename();
                log.info("media image compressed, filename={}, sourceBytes={}, storedBytes={}, dimensions={}x{}",
                        safeName(source.getOriginalFilename()), source.getSize(), processed.length,
                        scaled.getWidth(), scaled.getHeight());
                return new ByteArrayMultipartFile(source.getName(), filename, contentType, processed);
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            // A malformed image must not turn into an opaque 500 response. It
            // will subsequently go through the normal storage validation.
            log.warn("skip image compression because image cannot be decoded, filename={}",
                    safeName(source.getOriginalFilename()), exception);
            return source;
        }
    }

    private boolean isProcessableImage(MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        return "jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension);
    }

    private BufferedImage resizeIfNeeded(BufferedImage original) {
        int maxDimension = Math.max(1, properties.getMaxDimension());
        int sourceWidth = original.getWidth();
        int sourceHeight = original.getHeight();
        int longestEdge = Math.max(sourceWidth, sourceHeight);
        if (longestEdge <= maxDimension) {
            return original;
        }
        double scale = (double) maxDimension / longestEdge;
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        int imageType = original.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer unavailable");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(Math.max(0.1F, Math.min(1.0F, properties.getJpegQuality())));
            }
            writer.write(null, new IIOImage(rgb, null, null), parameters);
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private byte[] writePng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IOException("PNG writer unavailable");
            }
            return output.toByteArray();
        }
    }

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String withExtension(String filename, String extension) {
        String base = StringUtils.hasText(filename) ? filename.trim() : "image";
        int index = base.lastIndexOf('.');
        return (index < 0 ? base : base.substring(0, index)) + "." + extension;
    }

    private String safeName(String filename) {
        return StringUtils.hasText(filename) ? filename.replaceAll("[\\r\\n]", " ") : "upload-image";
    }
}
