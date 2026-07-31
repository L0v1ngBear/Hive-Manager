package my.hive.infrastructure.storage;

import my.hive.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FileStorageProviderRouter {

    private final Map<String, FileStorageProvider> providersByCode;
    private final FileStorageProvider selectedProvider;
    private final MediaUploadPreprocessor mediaUploadPreprocessor;

    public FileStorageProviderRouter(List<FileStorageProvider> providers,
                                     @Value("${storage.provider:local}") String configuredProvider,
                                     MediaUploadPreprocessor mediaUploadPreprocessor) {
        this.mediaUploadPreprocessor = mediaUploadPreprocessor;
        providersByCode = new LinkedHashMap<>();
        for (FileStorageProvider provider : providers) {
            String providerCode = normalizeProviderCode(provider.providerCode());
            if (providersByCode.putIfAbsent(providerCode, provider) != null) {
                throw new BusinessException("文件存储供应商编码重复：" + providerCode);
            }
        }

        String selectedCode = normalizeProviderCode(configuredProvider);
        selectedProvider = providersByCode.get(selectedCode);
        if (selectedProvider == null) {
            throw new BusinessException("不支持的文件存储供应商：" + selectedCode);
        }
    }

    public FileUploadResult upload(MultipartFile file, String tenantCode, String module) {
        return selectedProvider.upload(mediaUploadPreprocessor.prepare(file), tenantCode, module);
    }

    public void deleteQuietly(String objectKey) {
        selectedProvider.deleteQuietly(objectKey);
    }

    public void deleteQuietly(String providerCode, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        FileStorageProvider provider = StringUtils.hasText(providerCode)
                ? providersByCode.get(normalizeProviderCode(providerCode))
                : selectedProvider;
        if (provider != null) {
            provider.deleteQuietly(objectKey);
        }
    }

    public Resource load(String reference, String tenantCode, String module) {
        FileStorageProvider matchedProvider = null;
        for (FileStorageProvider provider : providersByCode.values()) {
            if (!provider.supportsReference(reference)) {
                continue;
            }
            if (matchedProvider != null) {
                throw new BusinessException("文件存储地址匹配到多个供应商");
            }
            matchedProvider = provider;
        }
        if (matchedProvider == null) {
            throw new BusinessException("文件存储地址不受支持");
        }
        return matchedProvider.load(reference, tenantCode, module);
    }

    private String normalizeProviderCode(String providerCode) {
        if (!StringUtils.hasText(providerCode)) {
            throw new BusinessException("文件存储供应商编码不能为空");
        }
        String normalized = providerCode.trim().toLowerCase(Locale.ROOT);
        // Existing production .env files commonly use the concise OSS name.
        if ("oss".equals(normalized) || "aliyun_oss".equals(normalized)) {
            return "aliyun-oss";
        }
        return normalized;
    }
}
