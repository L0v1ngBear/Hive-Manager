package my.hive.domain.installation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.security.InternalUploadUrlValidator;
import my.hive.infrastructure.storage.BusinessAttachmentService;
import my.hive.infrastructure.storage.BusinessAttachmentVO;
import my.hive.domain.installation.mapper.InstallationTaskInstallerMapper;
import my.hive.domain.installation.mapper.InstallationTaskMapper;
import my.hive.domain.installation.model.dto.InstallationTaskInstallerRequest;
import my.hive.domain.installation.model.dto.InstallationTaskPageRequest;
import my.hive.domain.installation.model.dto.InstallationTaskStatusUpdateRequest;
import my.hive.domain.installation.model.entity.InstallationTask;
import my.hive.domain.installation.model.entity.InstallationTaskInstaller;
import my.hive.domain.installation.model.enums.InstallationTaskStatusEnum;
import my.hive.domain.installation.model.vo.InstallationTaskInstallerVO;
import my.hive.domain.installation.model.vo.InstallationTaskVO;
import my.hive.domain.order.model.entity.SalesOrder;
import my.hive.domain.order.model.enums.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InstallationTaskService {

    private static final String ATTACHMENT_MODULE = "installation-task";
    private static final int MAX_INSTALLERS = 20;
    private static final int MAX_INSTALLER_NAME_LENGTH = 50;
    private static final int MAX_INSTALLER_PHONE_LENGTH = 40;

    @Resource
    private InstallationTaskMapper installationTaskMapper;

    @Resource
    private InstallationTaskInstallerMapper installationTaskInstallerMapper;

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
        Map<Long, List<InstallationTaskInstaller>> installersByTask = loadInstallers(
                tenantCode,
                sourcePage.getRecords().stream().map(InstallationTask::getId).toList()
        );
        Page<InstallationTaskVO> resultPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        resultPage.setPages(sourcePage.getPages());
        resultPage.setRecords(sourcePage.getRecords().stream()
                .map(task -> toVO(task, installersByTask.getOrDefault(task.getId(), List.of())))
                .toList());
        return resultPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrSyncFromInstallationReadyOrder(SalesOrder order) {
        if (order == null || !isInstallationReadyOrder(order.getStatus())) {
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
            task.setCreateTime(now);
        }
        if (OrderStatusEnum.COMPLETED.matches(order.getStatus()) && task.getOrderCompletedTime() == null) {
            task.setOrderCompletedTime(now);
        }
        copyOrderFields(order, task);
        task.setUpdateTime(now);
        if (create) {
            installationTaskMapper.insert(task);
        } else {
            installationTaskMapper.updateById(task);
        }
    }

    private boolean isInstallationReadyOrder(String orderStatus) {
        return OrderStatusEnum.PENDING_SHIP.matches(orderStatus)
                || OrderStatusEnum.SHIPPED.matches(orderStatus)
                || OrderStatusEnum.COMPLETED.matches(orderStatus);
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
        boolean installersProvided = request.getInstallers() != null;
        List<InstallationTaskInstallerRequest> installers = installersProvided
                ? normalizeInstallers(request.getInstallers(), status)
                : null;
        List<InstallationTaskInstaller> existingInstallers = installersProvided
                ? List.of()
                : loadInstallers(tenantCode, List.of(task.getId())).getOrDefault(task.getId(), List.of());
        if (!installersProvided
                && InstallationTaskStatusEnum.COMPLETED_ACCEPTED.matches(status.getCode())
                && existingInstallers.isEmpty()) {
            throw new BusinessException("已完成已验收状态至少需要一名安装人员");
        }
        task.setInstallationStatus(status.getCode());
        if (request.getExpressCompany() != null) {
            task.setExpressCompany(blankToNull(request.getExpressCompany()));
        }
        if (request.getExpressNo() != null) {
            task.setExpressNo(blankToNull(request.getExpressNo()));
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
            if (task.getAcceptedTime() == null) {
                task.setAcceptedTime(LocalDateTime.now());
            }
        } else {
            task.setAcceptedTime(null);
        }
        task.setUpdateTime(LocalDateTime.now());
        if (installationTaskMapper.updateById(task) != 1) {
            throw new BusinessException("安装任务更新失败");
        }
        List<InstallationTaskInstaller> savedInstallers = installersProvided
                ? replaceInstallers(tenantCode, task.getId(), installers)
                : existingInstallers;
        return toVO(task, savedInstallers);
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
        task.setIsInvoice(order.getIsInvoice());
        task.setCreator(order.getCreator());
        task.setOrderAttachmentName(order.getAttachmentName());
        task.setOrderAttachmentUrl(order.getAttachmentUrl());
        task.setOrderAttachmentSize(order.getAttachmentSize());
    }

    private InstallationTaskVO toVO(InstallationTask task, List<InstallationTaskInstaller> installers) {
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
        vo.setConstructionRemark(task.getConstructionRemark());
        vo.setSpecialExceptionNote(task.getSpecialExceptionNote());
        vo.setAttachmentName(task.getAttachmentName());
        vo.setAttachmentUrl(task.getAttachmentUrl());
        vo.setAttachmentSize(task.getAttachmentSize());
        vo.setOrderCompletedTime(task.getOrderCompletedTime());
        vo.setAcceptedTime(task.getAcceptedTime());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        vo.setInstallers(installers.stream().map(this::toInstallerVO).toList());
        return vo;
    }

    private Map<Long, List<InstallationTaskInstaller>> loadInstallers(String tenantCode, List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<InstallationTaskInstaller> installers = installationTaskInstallerMapper.selectList(
                new LambdaQueryWrapper<InstallationTaskInstaller>()
                        .eq(InstallationTaskInstaller::getTenantCode, tenantCode)
                        .in(InstallationTaskInstaller::getInstallationTaskId, taskIds)
                        .orderByAsc(InstallationTaskInstaller::getInstallationTaskId)
                        .orderByAsc(InstallationTaskInstaller::getSortOrder)
                        .orderByAsc(InstallationTaskInstaller::getId)
        );
        Map<Long, List<InstallationTaskInstaller>> grouped = new HashMap<>();
        for (InstallationTaskInstaller installer : installers) {
            grouped.computeIfAbsent(installer.getInstallationTaskId(), ignored -> new ArrayList<>()).add(installer);
        }
        return grouped;
    }

    private List<InstallationTaskInstallerRequest> normalizeInstallers(
            List<InstallationTaskInstallerRequest> source,
            InstallationTaskStatusEnum status) {
        List<InstallationTaskInstallerRequest> installers = source == null ? List.of() : source;
        if (installers.size() > MAX_INSTALLERS) {
            throw new BusinessException("安装人员最多 20 名");
        }
        List<InstallationTaskInstallerRequest> normalized = new ArrayList<>(installers.size());
        Set<List<String>> uniquePairs = new HashSet<>();
        for (InstallationTaskInstallerRequest sourceInstaller : installers) {
            if (sourceInstaller == null) {
                throw new BusinessException("安装人员姓名和联系电话不能为空");
            }
            String name = trimToEmpty(sourceInstaller.getName());
            String phone = trimToEmpty(sourceInstaller.getPhone());
            if (name.isEmpty()) {
                throw new BusinessException("安装人员姓名不能为空");
            }
            if (phone.isEmpty()) {
                throw new BusinessException("安装人员联系电话不能为空");
            }
            if (name.codePointCount(0, name.length()) > MAX_INSTALLER_NAME_LENGTH) {
                throw new BusinessException("安装人员姓名最多 50 字");
            }
            if (phone.codePointCount(0, phone.length()) > MAX_INSTALLER_PHONE_LENGTH) {
                throw new BusinessException("安装人员联系电话最多 40 字");
            }
            if (!uniquePairs.add(List.of(name, phone))) {
                throw new BusinessException("不能重复添加相同的安装人员和联系电话");
            }
            InstallationTaskInstallerRequest normalizedInstaller = new InstallationTaskInstallerRequest();
            normalizedInstaller.setName(name);
            normalizedInstaller.setPhone(phone);
            normalized.add(normalizedInstaller);
        }
        if (InstallationTaskStatusEnum.COMPLETED_ACCEPTED.matches(status.getCode()) && normalized.isEmpty()) {
            throw new BusinessException("已完成已验收状态至少需要一名安装人员");
        }
        return normalized;
    }

    private List<InstallationTaskInstaller> replaceInstallers(
            String tenantCode,
            Long installationTaskId,
            List<InstallationTaskInstallerRequest> installers) {
        installationTaskInstallerMapper.delete(new LambdaQueryWrapper<InstallationTaskInstaller>()
                .eq(InstallationTaskInstaller::getTenantCode, tenantCode)
                .eq(InstallationTaskInstaller::getInstallationTaskId, installationTaskId));
        LocalDateTime now = LocalDateTime.now();
        List<InstallationTaskInstaller> saved = new ArrayList<>(installers.size());
        for (int index = 0; index < installers.size(); index++) {
            InstallationTaskInstallerRequest source = installers.get(index);
            InstallationTaskInstaller installer = new InstallationTaskInstaller();
            installer.setTenantCode(tenantCode);
            installer.setInstallationTaskId(installationTaskId);
            installer.setInstallerName(source.getName());
            installer.setInstallerPhone(source.getPhone());
            installer.setSortOrder(index);
            installer.setCreateTime(now);
            installer.setUpdateTime(now);
            if (installationTaskInstallerMapper.insert(installer) != 1) {
                throw new BusinessException("安装人员保存失败");
            }
            saved.add(installer);
        }
        return saved;
    }

    private InstallationTaskInstallerVO toInstallerVO(InstallationTaskInstaller installer) {
        InstallationTaskInstallerVO vo = new InstallationTaskInstallerVO();
        vo.setId(installer.getId());
        vo.setName(installer.getInstallerName());
        vo.setPhone(installer.getInstallerPhone());
        vo.setSortOrder(installer.getSortOrder());
        return vo;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.strip();
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
