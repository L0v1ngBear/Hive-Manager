package my.hive.infrastructure.storage;

import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FileStorageProviderRouter {

    private final FileStorageProvider selectedProvider;

    public FileStorageProviderRouter(List<FileStorageProvider> providers,
                                     @Value("${storage.provider:local}") String configuredProvider) {
        Map<String, FileStorageProvider> providersByCode = new LinkedHashMap<>();
        for (FileStorageProvider provider : providers) {
            String providerCode = normalizeProviderCode(provider.providerCode());
            if (providersByCode.putIfAbsent(providerCode, provider) != null) {
                throw new BusinessException("Duplicate file storage provider code: " + providerCode);
            }
        }

        String selectedCode = normalizeProviderCode(configuredProvider);
        selectedProvider = providersByCode.get(selectedCode);
        if (selectedProvider == null) {
            throw new BusinessException("Unsupported file storage provider: " + selectedCode);
        }
    }

    public FileUploadResult upload(MultipartFile file, String tenantCode, String module) {
        return selectedProvider.upload(file, tenantCode, module);
    }

    public void deleteQuietly(String objectKey) {
        selectedProvider.deleteQuietly(objectKey);
    }

    private String normalizeProviderCode(String providerCode) {
        if (!StringUtils.hasText(providerCode)) {
            throw new BusinessException("File storage provider code is required");
        }
        return providerCode.trim().toLowerCase(Locale.ROOT);
    }
}
