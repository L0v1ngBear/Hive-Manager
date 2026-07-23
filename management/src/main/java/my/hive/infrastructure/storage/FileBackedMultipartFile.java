package my.hive.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Streams a completed upload from disk without buffering an entire video in memory. */
final class FileBackedMultipartFile implements MultipartFile {
    private final Path path;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    FileBackedMultipartFile(Path path, String name, String originalFilename, String contentType) {
        this.path = path;
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override public String getName() { return name; }
    @Override public String getOriginalFilename() { return originalFilename; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return getSize() == 0; }
    @Override public long getSize() { try { return Files.size(path); } catch (IOException e) { return 0; } }
    @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
    @Override public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
    @Override public void transferTo(File destination) throws IOException { Files.copy(path, destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
}
