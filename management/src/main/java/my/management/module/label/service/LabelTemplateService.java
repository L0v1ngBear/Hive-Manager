package my.management.module.label.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.BinaryFlagEnum;
import my.management.common.enums.CommonStatusEnum;
import my.management.common.enums.DeleteFlagEnum;
import my.management.module.label.mapper.LabelTemplateMapper;
import my.management.module.label.model.dto.LabelTemplateSaveRequest;
import my.management.module.label.model.entity.LabelTemplate;
import my.management.module.label.model.vo.LabelTemplateVO;
import my.management.module.label.model.vo.LabelTemplateVariableVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理端标签模板服务。
 * 管理端负责设计和维护模板，小程序端读取同一张 label_template 表进行打印。
 */
@Service
public class LabelTemplateService {

    private static final long MAX_FILE_SIZE = 1024 * 1024;
    private static final Pattern DOLLAR_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern BRACE_VARIABLE_PATTERN = Pattern.compile("(?<!\\$)\\{([^}]+)}");
    private static final BigDecimal DEFAULT_WIDTH_MM = new BigDecimal("70");
    private static final BigDecimal DEFAULT_HEIGHT_MM = new BigDecimal("50");
    private static final List<LabelTemplateVariableVO> LABEL_VARIABLES = List.of(
            new LabelTemplateVariableVO("条码", "barcode", "barcode", "CL20260421001"),
            new LabelTemplateVariableVO("型号", "modelCode", "text", "M-2026-A"),
            new LabelTemplateVariableVO("米数", "meters", "text", "120.50"),
            new LabelTemplateVariableVO("规格", "spec", "text", "160"),
            new LabelTemplateVariableVO("批次", "batchNo", "text", "BATCH-001"),
            new LabelTemplateVariableVO("入库时间", "inboundTime", "text", "2026-04-21"),
            new LabelTemplateVariableVO("客户", "customerName", "text", "示例客户")
    );
    private static final List<LabelTemplateVariableVO> RECEIPT_VARIABLES = List.of(
            new LabelTemplateVariableVO("单据编号", "orderNo", "text", "CK20260414001"),
            new LabelTemplateVariableVO("客户名称", "customerName", "text", "上海某服饰"),
            new LabelTemplateVariableVO("项目名称", "projectName", "text", "春季面料项目"),
            new LabelTemplateVariableVO("录单日期", "createDate", "text", "2026-04-14"),
            new LabelTemplateVariableVO("制单人", "operator", "text", "仓库管理员"),
            new LabelTemplateVariableVO("当前页码", "pageNo", "text", "1"),
            new LabelTemplateVariableVO("总页数", "totalPages", "text", "2"),
            new LabelTemplateVariableVO("货物名称", "modelCode", "text", "978-1-56915-43-9"),
            new LabelTemplateVariableVO("规格", "spec", "text", "160"),
            new LabelTemplateVariableVO("米数", "meters", "text", "120.50"),
            new LabelTemplateVariableVO("单价", "price", "text", "32.50"),
            new LabelTemplateVariableVO("金额", "amount", "text", "3916.25"),
            new LabelTemplateVariableVO("行备注", "remark", "text", "A区使用"),
            new LabelTemplateVariableVO("本页合计米数", "pageMeters", "text", "206.50"),
            new LabelTemplateVariableVO("本页小计金额", "pageAmount", "text", "7855.05"),
            new LabelTemplateVariableVO("总金额", "totalAmount", "text", "7855.05")
    );
    private static final Map<String, List<LabelTemplateVariableVO>> VARIABLE_MAP = Map.of(
            "label", LABEL_VARIABLES,
            "receipt", RECEIPT_VARIABLES
    );
    private static final String DEFAULT_LABEL_TEMPLATE = "SIZE 70 mm,50 mm\r\n"
            + "GAP 2 mm,0 mm\r\n"
            + "DIRECTION 1\r\n"
            + "CLS\r\n"
            + "TEXT 30,30,\"TSS24.BF2\",0,1,1,\"型号: ${modelCode}\"\r\n"
            + "TEXT 30,70,\"TSS24.BF2\",0,1,1,\"米数: ${meters} m\"\r\n"
            + "TEXT 30,110,\"TSS24.BF2\",0,1,1,\"规格: ${spec}\"\r\n"
            + "BARCODE 30,160,\"128\",80,1,0,2,2,\"${barcode}\"\r\n"
            + "TEXT 30,250,\"TSS24.BF2\",0,1,1,\"${barcode}\"\r\n"
            + "PRINT 1,1";
    private static final String DEFAULT_RECEIPT_TEMPLATE = """
            {
              "title": "面料销售码单",
              "subtitle": "出库凭证",
              "paperWidthMm": 215.9,
              "paperHeightMm": 139.7,
              "rowsPerPage": 7,
              "warehouse": "成品仓库",
              "notice": "请您与发货单核对本页货物，若有质量问题请在 15 天内告知；开剪后概不退换！感谢合作，共赢发展。",
              "showLogistics": true,
              "showSignature": true,
              "columns": [
                {"key": "modelCode", "label": "货物名称", "visible": true},
                {"key": "spec", "label": "规格", "visible": true},
                {"key": "meters", "label": "数量/米", "visible": true},
                {"key": "blank1", "label": "数量/米", "visible": true},
                {"key": "blank2", "label": "数量/米", "visible": true},
                {"key": "blank3", "label": "数量/米", "visible": true},
                {"key": "totalMeters", "label": "总米数", "visible": true},
                {"key": "price", "label": "单价", "visible": true},
                {"key": "amount", "label": "金额", "visible": true},
                {"key": "remark", "label": "备注", "visible": true}
              ],
              "variables": ["orderNo", "customerName", "projectName", "createDate", "operator", "modelCode", "spec", "meters", "price", "amount", "remark"]
            }
            """;

    @Resource
    private LabelTemplateMapper labelTemplateMapper;

    public List<LabelTemplateVariableVO> variables(String printType) {
        return VARIABLE_MAP.getOrDefault(resolvePrintType(printType), LABEL_VARIABLES);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<LabelTemplateVO> list(String printType) {
        LambdaQueryWrapper<LabelTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LabelTemplate::getStatus, 1);
        if (StringUtils.isNotBlank(printType)) {
            queryWrapper.eq(LabelTemplate::getPrintType, printType);
        }
        queryWrapper.orderByDesc(LabelTemplate::getIsDefault);
        queryWrapper.orderByDesc(LabelTemplate::getUpdateTime);
        List<LabelTemplate> templates = labelTemplateMapper.selectList(queryWrapper);
        if (templates.isEmpty()) {
            templates = List.of(createDefaultTemplate(resolvePrintType(printType)));
        }
        return templates.stream().map(this::toVO).toList();
    }

    public LabelTemplateVO detail(Long id) {
        LabelTemplate template = labelTemplateMapper.selectOne(new LambdaQueryWrapper<LabelTemplate>()
                .eq(LabelTemplate::getId, id));
        if (template == null) {
            throw new BusinessException("标签模板不存在");
        }
        return toVO(template);
    }

    public LabelTemplateVO defaultTemplate(String printType) {
        List<LabelTemplateVO> templates = list(StringUtils.isNotBlank(printType) ? printType : "label");
        if (templates.isEmpty()) {
            throw new BusinessException("暂无可用标签模板，请先在管理端上传");
        }
        return templates.stream()
                .filter(item -> BinaryFlagEnum.isYes(item.getIsDefault()))
                .findFirst()
                .orElse(templates.get(0));
    }

    /**
     * 为新租户预置标签和出库单模板，避免用户首次进入打印页面时缺少默认配置。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureDefaultsForTenant(String tenantCode, Long creatorId) {
        runIgnoringTenant(() -> {
            ensureDefaultTemplate(tenantCode, creatorId, "label");
            ensureDefaultTemplate(tenantCode, creatorId, "receipt");
            return null;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public LabelTemplateVO save(LabelTemplateSaveRequest request) {
        LabelTemplate template = resolveTemplateForSave(request.getId());
        template.setName(request.getName().trim());
        template.setPrintType(resolvePrintType(request.getPrintType()));
        template.setContent(request.getContent());
        template.setDesignJson(request.getDesignJson());
        template.setWidthMm(request.getWidthMm() == null ? DEFAULT_WIDTH_MM : request.getWidthMm());
        template.setHeightMm(request.getHeightMm() == null ? DEFAULT_HEIGHT_MM : request.getHeightMm());
        template.setVariables(String.join(",", extractVariables(request.getContent())));
        template.setIsDefault(BinaryFlagEnum.isYes(request.getIsDefault()) ? BinaryFlagEnum.YES.getCode() : BinaryFlagEnum.NO.getCode());
        template.setStatus(CommonStatusEnum.ENABLED.getCode());
        template.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());

        if (template.getId() == null) {
            template.setTenantCode(TenantPermissionContext.getTenantCode());
            template.setCreatorId(TenantPermissionContext.getUserId());
            labelTemplateMapper.insert(template);
        } else {
            labelTemplateMapper.updateById(template);
        }

        if (BinaryFlagEnum.isYes(template.getIsDefault())) {
            clearOtherDefault(template);
        }
        return toVO(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public LabelTemplateVO upload(MultipartFile file, String name, String printType, Integer isDefault) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的 PRN 模板文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("模板文件不能超过 1MB");
        }

        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".prn") && !lowerName.endsWith(".txt")) {
            throw new BusinessException("仅支持上传 .prn 或 .txt 模板文件");
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("模板文件读取失败");
        }
        if (!StringUtils.isNotBlank(content)) {
            throw new BusinessException("模板文件内容不能为空");
        }

        LabelTemplateSaveRequest request = new LabelTemplateSaveRequest();
        request.setName(StringUtils.isNotBlank(name) ? name : originalFilename);
        request.setPrintType(printType);
        request.setContent(content);
        request.setWidthMm(DEFAULT_WIDTH_MM);
        request.setHeightMm(DEFAULT_HEIGHT_MM);
        request.setIsDefault(isDefault);
        LabelTemplateVO vo = save(request);

        LabelTemplate update = new LabelTemplate();
        update.setId(vo.getId());
        update.setFileName(originalFilename);
        update.setFileSize(file.getSize());
        labelTemplateMapper.updateById(update);
        return detail(vo.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        LabelTemplate template = labelTemplateMapper.selectOne(new LambdaQueryWrapper<LabelTemplate>()
                .eq(LabelTemplate::getId, id));
        if (template == null) {
            throw new BusinessException("标签模板不存在");
        }
        template.setIsDefault(BinaryFlagEnum.YES.getCode());
        labelTemplateMapper.updateById(template);
        clearOtherDefault(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        LambdaUpdateWrapper<LabelTemplate> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabelTemplate::getId, id)
                .set(LabelTemplate::getStatus, 0);
        labelTemplateMapper.update(null, updateWrapper);
    }

    private LabelTemplate resolveTemplateForSave(Long id) {
        if (id == null) {
            return new LabelTemplate();
        }
        LabelTemplate template = labelTemplateMapper.selectOne(new LambdaQueryWrapper<LabelTemplate>()
                .eq(LabelTemplate::getId, id));
        if (template == null) {
            throw new BusinessException("标签模板不存在");
        }
        return template;
    }

    private LabelTemplate createDefaultTemplate(String printType) {
        if ("receipt".equals(printType)) {
            return createDefaultReceiptTemplate();
        }
        return createDefaultLabelTemplate();
    }

    private void ensureDefaultTemplate(String tenantCode, Long creatorId, String printType) {
        Long count = labelTemplateMapper.selectCount(new LambdaQueryWrapper<LabelTemplate>()
                .eq(LabelTemplate::getTenantCode, tenantCode)
                .eq(LabelTemplate::getPrintType, printType)
                .eq(LabelTemplate::getStatus, 1)
                .eq(LabelTemplate::getIsDeleted, 0));
        if (count != null && count > 0) {
            return;
        }
        if ("receipt".equals(printType)) {
            createDefaultReceiptTemplate(tenantCode, creatorId);
        } else {
            createDefaultLabelTemplate(tenantCode, creatorId);
        }
    }

    private LabelTemplate createDefaultLabelTemplate() {
        return createDefaultLabelTemplate(TenantPermissionContext.getTenantCode(), TenantPermissionContext.getUserId());
    }

    private LabelTemplate createDefaultLabelTemplate(String tenantCode, Long creatorId) {
        LabelTemplate template = new LabelTemplate();
        template.setTenantCode(tenantCode);
        template.setName("系统默认面料标签");
        template.setPrintType("label");
        template.setContent(DEFAULT_LABEL_TEMPLATE);
        template.setWidthMm(DEFAULT_WIDTH_MM);
        template.setHeightMm(DEFAULT_HEIGHT_MM);
        template.setVariables(String.join(",", extractVariables(DEFAULT_LABEL_TEMPLATE)));
        template.setFileName("system-default.prn");
        template.setFileSize((long) DEFAULT_LABEL_TEMPLATE.getBytes(StandardCharsets.UTF_8).length);
        template.setIsDefault(BinaryFlagEnum.YES.getCode());
        template.setStatus(CommonStatusEnum.ENABLED.getCode());
        template.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        template.setCreatorId(creatorId);
        labelTemplateMapper.insert(template);
        return template;
    }

    private LabelTemplate createDefaultReceiptTemplate() {
        return createDefaultReceiptTemplate(TenantPermissionContext.getTenantCode(), TenantPermissionContext.getUserId());
    }

    private LabelTemplate createDefaultReceiptTemplate(String tenantCode, Long creatorId) {
        LabelTemplate template = new LabelTemplate();
        template.setTenantCode(tenantCode);
        template.setName("系统默认出库单");
        template.setPrintType("receipt");
        template.setContent(DEFAULT_RECEIPT_TEMPLATE);
        template.setDesignJson(DEFAULT_RECEIPT_TEMPLATE);
        template.setWidthMm(new BigDecimal("215.9"));
        template.setHeightMm(new BigDecimal("139.7"));
        template.setVariables(String.join(",", RECEIPT_VARIABLES.stream().map(LabelTemplateVariableVO::getField).toList()));
        template.setFileName("system-default-receipt.json");
        template.setFileSize((long) DEFAULT_RECEIPT_TEMPLATE.getBytes(StandardCharsets.UTF_8).length);
        template.setIsDefault(BinaryFlagEnum.YES.getCode());
        template.setStatus(CommonStatusEnum.ENABLED.getCode());
        template.setIsDeleted(DeleteFlagEnum.NORMAL.getCode());
        template.setCreatorId(creatorId);
        labelTemplateMapper.insert(template);
        return template;
    }

    private void clearOtherDefault(LabelTemplate template) {
        LambdaUpdateWrapper<LabelTemplate> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabelTemplate::getPrintType, template.getPrintType())
                .ne(LabelTemplate::getId, template.getId())
                .set(LabelTemplate::getIsDefault, 0);
        labelTemplateMapper.update(null, updateWrapper);
    }

    private LabelTemplateVO toVO(LabelTemplate template) {
        LabelTemplateVO vo = new LabelTemplateVO();
        BeanUtils.copyProperties(template, vo);
        if (StringUtils.isNotBlank(template.getVariables())) {
            vo.setVariables(Arrays.stream(template.getVariables().split(","))
                    .filter(StringUtils::isNotBlank)
                    .toList());
        } else {
            vo.setVariables(Collections.emptyList());
        }
        return vo;
    }

    private String resolvePrintType(String printType) {
        return StringUtils.isNotBlank(printType) ? printType : "label";
    }

    private List<String> extractVariables(String content) {
        Set<String> variables = new LinkedHashSet<>();
        collectVariables(DOLLAR_VARIABLE_PATTERN.matcher(content), variables);
        collectVariables(BRACE_VARIABLE_PATTERN.matcher(content), variables);
        return variables.stream().toList();
    }

    private void collectVariables(Matcher matcher, Set<String> variables) {
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (StringUtils.isNotBlank(variable)) {
                variables.add(variable.trim());
            }
        }
    }

    private <T> T runIgnoringTenant(java.util.concurrent.Callable<T> callable) {
        boolean previous = TenantPermissionContext.isIgnoreTenant();
        TenantPermissionContext.setIgnoreTenant(true);
        try {
            return callable.call();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("模板默认数据初始化失败");
        } finally {
            if (previous) {
                TenantPermissionContext.setIgnoreTenant(true);
            } else {
                TenantPermissionContext.clearIgnore();
            }
        }
    }
}
