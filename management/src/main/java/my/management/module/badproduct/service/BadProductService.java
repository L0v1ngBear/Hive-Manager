package my.management.module.badproduct.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.OperationLogSkipContext;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.management.common.security.InternalUploadUrlValidator;
import my.management.common.utils.CodeGeneratorUtil;
import my.management.module.badproduct.mapper.BadProductMapper;
import my.management.module.badproduct.model.dto.BadProductPageRequest;
import my.management.module.badproduct.model.dto.BadProductProcessRequest;
import my.management.module.badproduct.model.dto.BadProductSaveRequest;
import my.management.module.badproduct.model.entity.BadProductRecord;
import my.management.module.badproduct.model.vo.BadProductVO;
import my.management.module.employee.mapper.EmployeeMapper;
import my.management.module.employee.model.entity.Employee;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 质量管理服务，复用小程序端同一张坏品表和处理规则。
 */
@Service
public class BadProductService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String BUSINESS_SCOPE_AFTER_SALES = "afterSales";
    private static final Set<String> AFTER_SALES_TYPES = Set.of(
            "after_sales",
            "return_exchange",
            "compensation",
            "customer_complaint"
    );

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Resource
    private BadProductMapper badProductMapper;

    @Resource
    private CodeGeneratorUtil codeGeneratorUtil;

    @Resource
    private EmployeeMapper employeeMapper;

    public PageResult<BadProductVO> page(BadProductPageRequest request) {
        if (request == null) {
            request = new BadProductPageRequest();
        }
        LambdaQueryWrapper<BadProductRecord> wrapper = new LambdaQueryWrapper<BadProductRecord>()
                .eq(BadProductRecord::getTenantCode, TenantPermissionContext.getTenantCode());

        String status = normalizeQueryValue(request.getStatus());
        String type = normalizeQueryValue(request.getType());
        String businessScope = normalizeQueryValue(request.getBusinessScope());
        String dateText = normalizeQueryValue(request.getDate());
        String keyword = normalizeQueryValue(request.getKeyword());
        String startDateText = normalizeQueryValue(request.getStartDate());
        String endDateText = normalizeQueryValue(request.getEndDate());

        boolean afterSalesScope = BUSINESS_SCOPE_AFTER_SALES.equalsIgnoreCase(businessScope);
        if (status != null && !"all".equals(status)) {
            wrapper.eq(BadProductRecord::getStatus, status);
        }
        if (type != null && !"all".equals(type)) {
            if (afterSalesScope != AFTER_SALES_TYPES.contains(type)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.eq(BadProductRecord::getType, type);
            }
        } else if (afterSalesScope) {
            wrapper.in(BadProductRecord::getType, AFTER_SALES_TYPES);
        } else {
            wrapper.notIn(BadProductRecord::getType, AFTER_SALES_TYPES);
        }
        if (keyword != null) {
            wrapper.and(w -> w.like(BadProductRecord::getDefectiveId, keyword)
                    .or().like(BadProductRecord::getOrderId, keyword)
                    .or().like(BadProductRecord::getDescription, keyword)
                    .or().like(BadProductRecord::getResponsiblePerson, keyword));
        }
        if (dateText != null) {
            LocalDate date = parseQueryDate(dateText, "登记日期");
            wrapper.ge(BadProductRecord::getCreateTime, date.atStartOfDay())
                    .lt(BadProductRecord::getCreateTime, date.plusDays(1).atStartOfDay());
        } else {
            if (startDateText != null) {
                wrapper.ge(BadProductRecord::getCreateTime, parseQueryDate(startDateText, "开始日期").atStartOfDay());
            }
            if (endDateText != null) {
                wrapper.lt(BadProductRecord::getCreateTime, parseQueryDate(endDateText, "结束日期").plusDays(1).atStartOfDay());
            }
        }

        wrapper.orderByDesc(BadProductRecord::getCreateTime);
        Page<BadProductRecord> page = badProductMapper.selectPage(
                new Page<>(safePageNum(request.getPageNum()), safePageSize(request.getPageSize())),
                wrapper
        );

        List<BadProductVO> records = page.getRecords().stream().map(this::toVO).toList();
        PageResult<BadProductVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(records);
        return result;
    }

    private LocalDate parseQueryDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (RuntimeException ex) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BadProductSaveRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Long userId = TenantPermissionContext.getUserId();
        Employee operator = userId == null ? null : employeeMapper.selectById(userId);

        BadProductRecord entity;
        BadProductRecord before = null;
        if (request.getDefectiveId() != null && !request.getDefectiveId().isBlank()) {
            entity = badProductMapper.selectOne(new LambdaQueryWrapper<BadProductRecord>()
                    .eq(BadProductRecord::getDefectiveId, request.getDefectiveId()));
            if (entity == null) {
                throw new BusinessException("质量记录不存在");
            }
            before = new BadProductRecord();
            BeanUtils.copyProperties(entity, before);
        } else {
            entity = new BadProductRecord();
            entity.setTenantCode(tenantCode);
            entity.setDefectiveId(codeGeneratorUtil.generateCode("DC", 4));
            entity.setCreatorId(userId);
            entity.setCreatorName(operator == null ? "未知用户" : operator.getName());
            entity.setStatus("pending");
            entity.setCreateTime(resolveBusinessCreateTime(request.getCreateTime()));
        }

        entity.setOrderId(blankToNull(request.getOrderId()));
        entity.setType(request.getType());
        entity.setQuantity(request.getQuantity());
        entity.setLossAmount(request.getLossAmount());
        entity.setDescription(blankToNull(request.getDescription()));
        entity.setResponsiblePerson(blankToNull(request.getResponsiblePerson()));
        entity.setProcessMeasure(blankToNull(request.getProcessMeasure()));
        entity.setImprovementPlan(blankToNull(request.getImprovementPlan()));
        entity.setAttachmentName(normalizeAttachmentName(request.getAttachmentName(), request.getAttachmentUrl()));
        entity.setAttachmentUrl(normalizeBadProductAttachmentUrl(request.getAttachmentUrl(), tenantCode));
        entity.setAttachmentSize(normalizeAttachmentSize(request.getAttachmentSize(), entity.getAttachmentUrl()));
        if (before != null && request.getCreateTime() != null) {
            entity.setCreateTime(resolveBusinessCreateTime(request.getCreateTime()));
        }

        if (entity.getId() == null) {
            badProductMapper.insert(entity);
        } else {
            boolean businessChanged = badProductContentChanged(before, entity);
            if (!businessChanged) {
                entity.setUpdateTime(before.getUpdateTime());
            }
            badProductMapper.updateById(entity);
            if (!businessChanged) {
                OperationLogSkipContext.skipCurrent();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void process(BadProductProcessRequest request) {
        BadProductRecord entity = badProductMapper.selectOne(new LambdaQueryWrapper<BadProductRecord>()
                .eq(BadProductRecord::getDefectiveId, request.getDefectiveId()));
        if (entity == null) {
            throw new BusinessException("质量记录不存在");
        }
        entity.setStatus("processed");
        entity.setProcessMethod(request.getMethod());
        entity.setProcessRemark(blankToNull(request.getRemark()));
        badProductMapper.updateById(entity);
    }

    private BadProductVO toVO(BadProductRecord entity) {
        BadProductVO vo = new BadProductVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCreator(entity.getCreatorName());
        return vo;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeAttachmentName(String attachmentName, String attachmentUrl) {
        if (blankToNull(attachmentUrl) == null) {
            return null;
        }
        String normalized = blankToNull(attachmentName);
        if (normalized == null) {
            return "quality-attachment";
        }
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }

    private String normalizeBadProductAttachmentUrl(String attachmentUrl, String tenantCode) {
        if (blankToNull(attachmentUrl) == null) {
            return null;
        }
        return InternalUploadUrlValidator.normalizeStoredUploadUrl(
                attachmentUrl,
                resolveContextPath(),
                tenantCode,
                "bad-product"
        );
    }

    private Long normalizeAttachmentSize(Long attachmentSize, String attachmentUrl) {
        if (blankToNull(attachmentUrl) == null) {
            return null;
        }
        if (attachmentSize == null || attachmentSize < 0) {
            return null;
        }
        return attachmentSize;
    }

    private String resolveContextPath() {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath.trim())) {
            return "";
        }
        return contextPath.trim();
    }

    private String normalizeQueryValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private LocalDateTime resolveBusinessCreateTime(LocalDateTime value) {
        LocalDateTime now = LocalDateTime.now();
        if (value == null) {
            return now;
        }
        if (value.isAfter(now)) {
            throw new BusinessException("登记时间不能晚于当前时间");
        }
        return value;
    }

    private boolean badProductContentChanged(BadProductRecord before, BadProductRecord after) {
        if (before == null || after == null) {
            return true;
        }
        return !Objects.equals(before.getOrderId(), after.getOrderId())
                || !Objects.equals(before.getType(), after.getType())
                || !sameBigDecimal(before.getQuantity(), after.getQuantity())
                || !sameBigDecimal(before.getLossAmount(), after.getLossAmount())
                || !Objects.equals(before.getDescription(), after.getDescription())
                || !Objects.equals(before.getResponsiblePerson(), after.getResponsiblePerson())
                || !Objects.equals(before.getProcessMeasure(), after.getProcessMeasure())
                || !Objects.equals(before.getImprovementPlan(), after.getImprovementPlan())
                || !Objects.equals(before.getAttachmentName(), after.getAttachmentName())
                || !Objects.equals(before.getAttachmentUrl(), after.getAttachmentUrl())
                || !Objects.equals(before.getAttachmentSize(), after.getAttachmentSize());
    }

    private boolean sameBigDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }
}
