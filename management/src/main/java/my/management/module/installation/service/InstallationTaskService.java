package my.management.module.installation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.common.security.InternalUploadUrlValidator;
import my.management.common.storage.BusinessAttachmentService;
import my.management.common.storage.BusinessAttachmentVO;
import my.management.module.installation.mapper.InstallationTaskMapper;
import my.management.module.installation.model.dto.InstallationTaskPageRequest;
import my.management.module.installation.model.dto.InstallationTaskStatusUpdateRequest;
import my.management.module.installation.model.entity.InstallationTask;
import my.management.module.installation.model.enums.InstallationTaskStatusEnum;
import my.management.module.installation.model.vo.InstallationTaskVO;
import my.management.module.order.model.entity.SalesOrder;
import my.management.module.order.model.enums.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class InstallationTaskService {

    private static final String ATTACHMENT_MODULE = "installation-task";

    @Resource
    private InstallationTaskMapper installationTaskMapper;

    @Resource
    private BusinessAttachmentService businessAttachmentService;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public Page<InstallationTaskVO> page(InstallationTaskPageRequest request) {
        InstallationTaskPageRequest safeRequest = request == null ? new InstallationTaskPageRequest() : request;
        Long current = safeRequest.getCurrent() == null || safeRequest.getCurrent() < 1 ? 1L : safeRequest.getCurrent();
        Long size = safeRequest.getSize() == null || safeRequest.getSize() < 1 ? 10L : Math.min(safeRequest.getSize(), 100L);
        String tenantCode = TenantPermissionContext.getTenantCode();

        LambdaQueryWrapper<InstallationTask> wrapper = new LambdaQueryWrapper<InstallationTask>()
                .eq(InstallationTask::getTenantCode, tenantCode)
                .orderByDesc(InstallationTask::getUpdateTime)
                .orderByDesc(InstallationTask::getId);
        if (StringUtils.hasText(safeRequest.getStatus())) {
            wrapper.eq(InstallationTask::getInstallationStatus, safeRequest.getStatus().trim());
        }
        if (StringUtils.hasText(safeRequest.getCustomerName())) {
            wrapper.like(InstallationTask::getCustomerName, safeRequest.getCustomerName().trim());
        }
        if (StringUtils.hasText(safeRequest.getProjectName())) {
            wrapper.like(InstallationTask::getProjectName, safeRequest.getProjectName().trim());
        }
        if (StringUtils.hasText(safeRequest.getKeyword())) {
            String keyword = safeRequest.getKeyword().trim();
            wrapper.and(w -> w.like(InstallationTask::getOrderId, keyword)
                    .or().like(InstallationTask::getCustomerName, keyword)
                    .or().like(InstallationTask::getProjectName, keyword)
                    .or().like(InstallationTask::getBrandName, keyword)
                    .or().like(InstallationTask::getGoodsDesc, keyword)
                    .or().like(InstallationTask::getExpressNo, keyword));
        }
        Page<InstallationTask> sourcePage = installationTaskMapper.selectPage(new Page<>(current, size), wrapper);
        Page<InstallationTaskVO> resultPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        resultPage.setPages(sourcePage.getPages());
        resultPage.setRecords(sourcePage.getRecords().stream().map(this::toVO).toList());
        return resultPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrSyncFromCompletedOrder(SalesOrder order) {
        if (order == null || !OrderStatusEnum.COMPLETED.matches(order.getStatus())) {
            return;
        }
        InstallationTask task = installationTaskMapper.selectOne(new LambdaQueryWrapper<InstallationTask>()
                .eq(InstallationTask::getTenantCode, order.getTenantCode())
                .eq(InstallationTask::getOrderId, order.getOrderId())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        boolean create = task == null;
        if (create) {
            task = new InstallationTask();
            task.setTenantCode(order.getTenantCode());
            task.setOrderId(order.getOrderId());
            task.setInstallationStatus(InstallationTaskStatusEnum.PRODUCTION_COMPLETED.getCode());
            task.setOrderCompletedTime(now);
            task.setCreateTime(now);
        }
        copyOrderFields(order, task);
        task.setUpdateTime(now);
        if (create) {
            installationTaskMapper.insert(task);
        } else {
            installationTaskMapper.updateById(task);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public InstallationTaskVO updateStatus(InstallationTaskStatusUpdateRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        InstallationTask task = installationTaskMapper.selectOne(new LambdaQueryWrapper<InstallationTask>()
                .eq(InstallationTask::getTenantCode, tenantCode)
                .eq(InstallationTask::getId, request.getId())
                .last("LIMIT 1"));
        if (task == null) {
            throw new BusinessException("安装任务不存在");
        }
        InstallationTaskStatusEnum status = InstallationTaskStatusEnum.require(request.getStatus());
        task.setInstallationStatus(status.getCode());
        if (request.getExpressCompany() != null) {
            task.setExpressCompany(blankToNull(request.getExpressCompany()));
        }
        if (request.getExpressNo() != null) {
            task.setExpressNo(blankToNull(request.getExpressNo()));
        }
        if (request.getConstructionPersonnel() != null) {
            task.setConstructionPersonnel(blankToNull(request.getConstructionPersonnel()));
        }
        if (request.getConstructionPhone() != null) {
            task.setConstructionPhone(blankToNull(request.getConstructionPhone()));
        }
        if (request.getConstructionRemark() != null) {
            task.setConstructionRemark(blankToNull(request.getConstructionRemark()));
        }
        if (request.getSpecialExceptionNote() != null) {
            task.setSpecialExceptionNote(blankToNull(request.getSpecialExceptionNote()));
        }
        if (request.getAttachmentName() != null) {
            task.setAttachmentName(blankToNull(request.getAttachmentName()));
        }
        if (request.getAttachmentUrl() != null) {
            task.setAttachmentUrl(normalizeAttachmentUrl(request.getAttachmentUrl(), tenantCode));
        }
        if (request.getAttachmentSize() != null) {
            task.setAttachmentSize(normalizeAttachmentSize(request.getAttachmentSize()));
        }
        if (InstallationTaskStatusEnum.SHIPPED_PENDING_INSTALL.matches(status.getCode())
                && (!StringUtils.hasText(task.getExpressCompany()) || !StringUtils.hasText(task.getExpressNo()))) {
            throw new BusinessException("已发货待安装状态需要填写物流信息");
        }
        if (InstallationTaskStatusEnum.COMPLETED_ACCEPTED.matches(status.getCode())) {
            if (!StringUtils.hasText(task.getConstructionPersonnel())) {
                throw new BusinessException("已完成已验收状态需要填写施工人员信息");
            }
            if (task.getAcceptedTime() == null) {
                task.setAcceptedTime(LocalDateTime.now());
            }
        } else {
            task.setAcceptedTime(null);
        }
        task.setUpdateTime(LocalDateTime.now());
        installationTaskMapper.updateById(task);
        return toVO(task);
    }

    public BusinessAttachmentVO uploadAttachment(MultipartFile file) {
        return businessAttachmentService.upload(file, ATTACHMENT_MODULE);
    }

    public org.springframework.core.io.Resource loadAttachment(String url) {
        return businessAttachmentService.load(url, ATTACHMENT_MODULE);
    }

    private void copyOrderFields(SalesOrder order, InstallationTask task) {
        task.setOrderStatus(order.getStatus());
        task.setCustomerName(order.getCustomerName());
        task.setCustomerPhone(order.getCustomerPhone());
        task.setProjectName(order.getProjectName());
        task.setBrandName(order.getBrandName());
        task.setOrderCategory(order.getOrderCategory());
        task.setGoodsDesc(order.getGoodsDesc());
        task.setTotalQuantity(order.getTotalQuantity());
        task.setInformationChannel(order.getInformationChannel());
        task.setExpressCompany(order.getExpressCompany());
        task.setExpressNo(order.getExpressNo());
        task.setIsInvoice(order.getIsInvoice());
        task.setCreator(order.getCreator());
        task.setRemark(order.getRemark());
        task.setOrderAttachmentName(order.getAttachmentName());
        task.setOrderAttachmentUrl(order.getAttachmentUrl());
        task.setOrderAttachmentSize(order.getAttachmentSize());
    }

    private InstallationTaskVO toVO(InstallationTask task) {
        InstallationTaskVO vo = new InstallationTaskVO();
        vo.setId(task.getId());
        vo.setOrderId(task.getOrderId());
        vo.setOrderStatus(task.getOrderStatus());
        vo.setInstallationStatus(task.getInstallationStatus());
        vo.setCustomerName(task.getCustomerName());
        vo.setCustomerPhone(task.getCustomerPhone());
        vo.setProjectName(task.getProjectName());
        vo.setBrandName(task.getBrandName());
        vo.setOrderCategory(task.getOrderCategory());
        vo.setGoodsDesc(task.getGoodsDesc());
        vo.setTotalQuantity(task.getTotalQuantity());
        vo.setInformationChannel(task.getInformationChannel());
        vo.setExpressCompany(task.getExpressCompany());
        vo.setExpressNo(task.getExpressNo());
        vo.setIsInvoice(task.getIsInvoice());
        vo.setCreator(task.getCreator());
        vo.setRemark(task.getRemark());
        vo.setOrderAttachmentName(task.getOrderAttachmentName());
        vo.setOrderAttachmentUrl(task.getOrderAttachmentUrl());
        vo.setOrderAttachmentSize(task.getOrderAttachmentSize());
        vo.setConstructionPersonnel(task.getConstructionPersonnel());
        vo.setConstructionPhone(task.getConstructionPhone());
        vo.setConstructionRemark(task.getConstructionRemark());
        vo.setSpecialExceptionNote(task.getSpecialExceptionNote());
        vo.setAttachmentName(task.getAttachmentName());
        vo.setAttachmentUrl(task.getAttachmentUrl());
        vo.setAttachmentSize(task.getAttachmentSize());
        vo.setOrderCompletedTime(task.getOrderCompletedTime());
        vo.setAcceptedTime(task.getAcceptedTime());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeAttachmentUrl(String attachmentUrl, String tenantCode) {
        if (!StringUtils.hasText(attachmentUrl)) {
            return null;
        }
        return InternalUploadUrlValidator.normalizeStoredUploadUrl(
                attachmentUrl,
                resolveContextPath(),
                tenantCode,
                ATTACHMENT_MODULE
        );
    }

    private Long normalizeAttachmentSize(Long attachmentSize) {
        if (attachmentSize == null) {
            return null;
        }
        if (attachmentSize < 0) {
            throw new BusinessException("Invalid attachment size");
        }
        return attachmentSize;
    }

    private String resolveContextPath() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath.trim())) {
            return "";
        }
        return contextPath.trim();
    }
}
