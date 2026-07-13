package my.management.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.module.tenant.mapper.TenantFieldConfigMapper;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.dto.TenantFieldConfigSaveRequest;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.model.entity.TenantFieldConfig;
import my.management.module.tenant.model.vo.TenantFieldConfigVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TenantFieldConfigService {

    private static final Pattern MODULE_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_.:-]{0,63}$");
    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.:-]{0,79}$");
    private static final Pattern CUSTOM_FIELD_KEY_PATTERN = Pattern.compile("^custom_[a-zA-Z][a-zA-Z0-9_]{0,63}$");
    private static final String DEFAULT_FIELD_TYPE = "text";
    private static final Set<String> SUPPORTED_FIELD_TYPES = Set.of("text", "number", "date", "datetime", "select");
    private static final int MAX_FIELDS_PER_SAVE = 80;
    private static final int MAX_CUSTOM_FIELDS_PER_MODULE = 30;
    private static final Map<String, Set<String>> SUPPORTED_FIELDS = Map.of(
            "inventory", Set.of("modelCode", "spec", "barCode", "totalMeters", "remainingMeters", "location", "status", "updateTime"),
            "receipt", Set.of("modelCode", "spec", "meters", "price", "amount", "remark"),
            "employee", Set.of("name", "empNo", "employeeType", "phone", "email", "departmentName", "positionName", "leaderName", "entryDate", "status", "remark"),
            "customer", Set.of("customerName", "customerType", "contactName", "contactPhone", "projectName", "projectOwner", "projectCount", "constructionArea")
    );

    @Resource
    private TenantFieldConfigMapper tenantFieldConfigMapper;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private ObjectMapper objectMapper;

    public List<TenantFieldConfigVO> list(String tenantCode, String moduleCode) {
        ensureDeveloperAccess();
        String safeTenantCode = requireTenantCode(tenantCode);
        String safeModuleCode = requireModuleCode(moduleCode);
        ensureTenantExists(safeTenantCode);
        return listByTenant(safeTenantCode, safeModuleCode);
    }

    public List<TenantFieldConfigVO> listForCurrentTenant(String moduleCode) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("请先选择租户");
        }
        String safeModuleCode = requireModuleCode(moduleCode);
        return listByTenant(requireTenantCode(tenantCode), safeModuleCode);
    }

    public Map<String, String> currentFieldLabelMap(String moduleCode) {
        String safeModuleCode = requireModuleCode(moduleCode);
        Map<String, String> labels = new HashMap<>();
        for (TenantFieldConfigVO config : listForCurrentTenant(safeModuleCode)) {
            if (config == null || !isSupportedOrCustomFieldKey(safeModuleCode, config.getFieldKey())) {
                continue;
            }
            String label = config.getFieldLabel();
            if (label != null && !label.trim().isBlank()) {
                labels.put(config.getFieldKey(), label.trim());
            }
        }
        return labels;
    }

    public List<TenantFieldConfigVO> currentCustomFieldConfigs(String moduleCode) {
        String safeModuleCode = requireModuleCode(moduleCode);
        return listForCurrentTenant(safeModuleCode).stream()
                .filter(config -> config != null && Boolean.TRUE.equals(config.getCustom()))
                .filter(config -> config.getVisible() == null || config.getVisible())
                .toList();
    }

    private List<TenantFieldConfigVO> listByTenant(String tenantCode, String moduleCode) {
        return tenantFieldConfigMapper.selectList(new LambdaQueryWrapper<TenantFieldConfig>()
                        .eq(TenantFieldConfig::getTenantCode, tenantCode)
                        .eq(TenantFieldConfig::getModuleCode, moduleCode)
                        .orderByAsc(TenantFieldConfig::getSortNo)
                        .orderByAsc(TenantFieldConfig::getId))
                .stream()
                .filter(config -> isSupportedOrCustomFieldKey(moduleCode, config.getFieldKey()))
                .map(this::toVO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<TenantFieldConfigVO> save(String tenantCode, TenantFieldConfigSaveRequest request) {
        ensureDeveloperAccess();
        String safeTenantCode = requireTenantCode(tenantCode);
        String safeModuleCode = requireModuleCode(request == null ? null : request.getModuleCode());
        ensureTenantExists(safeTenantCode);
        List<TenantFieldConfigSaveRequest.FieldItem> fields = request == null ? List.of() : request.getFields();
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("字段配置不能为空");
        }
        if (fields.size() > MAX_FIELDS_PER_SAVE) {
            throw new BusinessException("单次最多保存 " + MAX_FIELDS_PER_SAVE + " 个字段配置");
        }

        LocalDateTime now = LocalDateTime.now();
        Set<String> submittedFieldKeys = new HashSet<>();
        Set<String> submittedFieldLabels = new HashSet<>();
        int customFieldCount = 0;
        for (int i = 0; i < fields.size(); i++) {
            TenantFieldConfigSaveRequest.FieldItem item = fields.get(i);
            String fieldKey = requireFieldKey(item == null ? null : item.getFieldKey());
            ensureSupportedOrCustomFieldKey(safeModuleCode, fieldKey);
            boolean customField = isCustomFieldKey(fieldKey);
            if (customField && ++customFieldCount > MAX_CUSTOM_FIELDS_PER_MODULE) {
                throw new BusinessException("单个模块最多允许 " + MAX_CUSTOM_FIELDS_PER_MODULE + " 个租户自定义字段");
            }
            if (!submittedFieldKeys.add(fieldKey)) {
                throw new BusinessException("字段配置重复：" + fieldKey);
            }
            String fieldLabel = requireFieldLabel(item.getFieldLabel());
            if (!submittedFieldLabels.add(normalizeFieldLabelKey(fieldLabel))) {
                throw new BusinessException("字段显示名称重复：" + fieldLabel);
            }
            validateOptionsJson(item.getOptionsJson());
            boolean visible = item.getVisible() == null || item.getVisible();
            boolean required = Boolean.TRUE.equals(item.getRequired());
            if (!visible && required) {
                throw new BusinessException("必填字段不能同时设置为隐藏：" + fieldLabel);
            }
            String fieldType = normalizeFieldType(item.getFieldType());

            TenantFieldConfig config = tenantFieldConfigMapper.selectOne(new LambdaQueryWrapper<TenantFieldConfig>()
                    .eq(TenantFieldConfig::getTenantCode, safeTenantCode)
                    .eq(TenantFieldConfig::getModuleCode, safeModuleCode)
                    .eq(TenantFieldConfig::getFieldKey, fieldKey)
                    .last("LIMIT 1"));
            boolean insert = config == null;
            if (insert) {
                config = new TenantFieldConfig();
                config.setTenantCode(safeTenantCode);
                config.setModuleCode(safeModuleCode);
                config.setFieldKey(fieldKey);
                config.setCreateTime(now);
            }
            config.setFieldLabel(fieldLabel);
            config.setVisibleFlag(BinaryFlagEnum.codeOf(visible));
            config.setRequiredFlag(BinaryFlagEnum.codeOf(required));
            config.setSortNo(safeSortNo(item.getSortNo(), i));
            config.setFieldType(fieldType);
            config.setOptionsJson(blankToNull(item.getOptionsJson()));
            config.setRemark(blankToNull(item.getRemark()));
            config.setUpdateTime(now);
            if (insert) {
                tenantFieldConfigMapper.insert(config);
            } else {
                tenantFieldConfigMapper.updateById(config);
            }
        }
        return list(safeTenantCode, safeModuleCode);
    }

    private void ensureTenantExists(String tenantCode) {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantCode, tenantCode)
                .eq(Tenant::getDeleted, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
    }

    private String requireTenantCode(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException("租户编码不能为空");
        }
        String tenantCode = value.trim().toUpperCase(Locale.ROOT);
        if (!tenantCode.matches("^[A-Z0-9_]{1,50}$")) {
            throw new BusinessException("租户编码格式不正确");
        }
        return tenantCode;
    }

    private String requireModuleCode(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException("模块编码不能为空");
        }
        String moduleCode = value.trim();
        if (!MODULE_CODE_PATTERN.matcher(moduleCode).matches()) {
            throw new BusinessException("模块编码格式不正确");
        }
        return moduleCode;
    }

    private void ensureSupportedOrCustomFieldKey(String moduleCode, String fieldKey) {
        if (!SUPPORTED_FIELDS.containsKey(moduleCode)) {
            throw new BusinessException("moduleCode is not supported for page field configuration");
        }
        if (!isSupportedOrCustomFieldKey(moduleCode, fieldKey)) {
            throw new BusinessException("fieldKey is not supported by backend: " + fieldKey);
        }
    }

    private boolean isSupportedOrCustomFieldKey(String moduleCode, String fieldKey) {
        return isSupportedFieldKey(moduleCode, fieldKey) || isCustomFieldKey(fieldKey);
    }

    private boolean isSupportedFieldKey(String moduleCode, String fieldKey) {
        Set<String> supportedFields = SUPPORTED_FIELDS.get(moduleCode);
        return supportedFields != null && supportedFields.contains(fieldKey);
    }

    private boolean isCustomFieldKey(String fieldKey) {
        return fieldKey != null && CUSTOM_FIELD_KEY_PATTERN.matcher(fieldKey).matches();
    }

    private String requireFieldKey(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException("字段编码不能为空");
        }
        String fieldKey = value.trim();
        if (!FIELD_KEY_PATTERN.matcher(fieldKey).matches()) {
            throw new BusinessException("字段编码格式不正确");
        }
        return fieldKey;
    }

    private String requireFieldLabel(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException("字段名称不能为空");
        }
        String label = value.trim();
        if (label.length() > 80) {
            throw new BusinessException("字段名称过长");
        }
        if (label.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException("字段名称不能包含控制字符");
        }
        return label;
    }

    private String normalizeFieldLabelKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void validateOptionsJson(String value) {
        if (value == null || value.trim().isBlank()) {
            return;
        }
        try {
            objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new BusinessException("字段选项格式不正确");
        }
    }

    private String normalizeFieldType(String value) {
        String fieldType = value == null || value.trim().isBlank()
                ? DEFAULT_FIELD_TYPE
                : value.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FIELD_TYPES.contains(fieldType)) {
            throw new BusinessException("字段类型不支持：" + fieldType);
        }
        return fieldType;
    }

    private Integer safeSortNo(Integer value, int index) {
        if (value == null) {
            return index + 1;
        }
        return Math.max(0, Math.min(value, 9999));
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private TenantFieldConfigVO toVO(TenantFieldConfig config) {
        TenantFieldConfigVO vo = new TenantFieldConfigVO();
        BeanUtils.copyProperties(config, vo);
        vo.setVisible(BinaryFlagEnum.isYes(config.getVisibleFlag()));
        vo.setRequired(BinaryFlagEnum.isYes(config.getRequiredFlag()));
        vo.setCustom(isCustomFieldKey(config.getFieldKey()));
        vo.setFieldType(config.getFieldType() == null || config.getFieldType().isBlank()
                ? DEFAULT_FIELD_TYPE
                : config.getFieldType());
        return vo;
    }

    private void ensureDeveloperAccess() {
        throw new BusinessException("字段配置维护入口已下线");
    }
}
