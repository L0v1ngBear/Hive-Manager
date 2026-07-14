package my.management.module.equipment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.management.module.equipment.mapper.EquipmentDeviceMapper;
import my.management.module.equipment.mapper.EquipmentInspectionRecordMapper;
import my.management.module.equipment.model.dto.EquipmentInspectionSubmitRequest;
import my.management.module.equipment.model.dto.EquipmentPageRequest;
import my.management.module.equipment.model.dto.EquipmentRecordPageRequest;
import my.management.module.equipment.model.dto.EquipmentSaveRequest;
import my.management.module.equipment.model.entity.EquipmentDevice;
import my.management.module.equipment.model.entity.EquipmentInspectionRecord;
import my.management.module.equipment.model.vo.EquipmentDeviceVO;
import my.management.module.equipment.model.vo.EquipmentInspectionRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class EquipmentService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final String RESULT_NORMAL = "normal";
    private static final String RESULT_ABNORMAL = "abnormal";
    private static final DateTimeFormatter CODE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private EquipmentDeviceMapper equipmentDeviceMapper;

    @Resource
    private EquipmentInspectionRecordMapper inspectionRecordMapper;

    public Page<EquipmentDeviceVO> page(EquipmentPageRequest request) {
        EquipmentPageRequest safeRequest = request == null ? new EquipmentPageRequest() : request;
        String tenantCode = TenantPermissionContext.getTenantCode();
        LambdaQueryWrapper<EquipmentDevice> wrapper = new LambdaQueryWrapper<EquipmentDevice>()
                .eq(EquipmentDevice::getTenantCode, tenantCode);

        String keyword = cleanText(safeRequest.getKeyword());
        if (keyword != null) {
            wrapper.and(w -> w
                    .like(EquipmentDevice::getEquipmentCode, keyword)
                    .or().like(EquipmentDevice::getEquipmentName, keyword)
                    .or().like(EquipmentDevice::getEquipmentType, keyword)
                    .or().like(EquipmentDevice::getLocation, keyword)
                    .or().like(EquipmentDevice::getResponsiblePerson, keyword));
        }

        String status = normalizeStatus(safeRequest.getStatus(), false);
        if (status != null) {
            wrapper.eq(EquipmentDevice::getStatus, status);
        }

        wrapper.orderByDesc(EquipmentDevice::getUpdateTime).orderByDesc(EquipmentDevice::getId);
        Page<EquipmentDevice> entityPage = equipmentDeviceMapper.selectPage(
                new Page<>(safePageNum(safeRequest.getPageNum()), safePageSize(safeRequest.getPageSize())),
                wrapper);
        Page<EquipmentDeviceVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setPages(entityPage.getPages());
        result.setRecords(entityPage.getRecords().stream().map(this::toDeviceVO).toList());
        return result;
    }

    public EquipmentDeviceVO detail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("设备ID不能为空");
        }
        EquipmentDevice device = equipmentDeviceMapper.selectOne(new LambdaQueryWrapper<EquipmentDevice>()
                .eq(EquipmentDevice::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(EquipmentDevice::getId, id)
                .last("LIMIT 1"));
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        return toDeviceVO(device);
    }

    public EquipmentDeviceVO scanTarget(String equipmentCode) {
        EquipmentDevice device = findDeviceByCode(equipmentCode, true);
        return toDeviceVO(device);
    }

    @Transactional(rollbackFor = Exception.class)
    public EquipmentDeviceVO save(EquipmentSaveRequest request) {
        if (request == null) {
            throw new BusinessException("设备参数不能为空");
        }
        String tenantCode = TenantPermissionContext.getTenantCode();
        String equipmentName = cleanText(request.getEquipmentName());
        if (equipmentName == null) {
            throw new BusinessException("设备名称不能为空");
        }

        boolean creating = request.getId() == null;
        EquipmentDevice entity = creating ? new EquipmentDevice() : equipmentDeviceMapper.selectOne(
                new LambdaQueryWrapper<EquipmentDevice>()
                        .eq(EquipmentDevice::getTenantCode, tenantCode)
                        .eq(EquipmentDevice::getId, request.getId())
                        .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException("设备不存在");
        }

        String equipmentCode;
        if (creating) {
            equipmentCode = cleanText(request.getEquipmentCode());
            if (equipmentCode == null) {
                equipmentCode = generateEquipmentCode();
            }
        } else {
            equipmentCode = entity.getEquipmentCode();
            String requestedCode = cleanText(request.getEquipmentCode());
            if (requestedCode != null && !requestedCode.equals(equipmentCode)) {
                throw new BusinessException("设备编码创建后不可修改，请停用旧设备后新建设备");
            }
        }

        Long duplicateCount = equipmentDeviceMapper.selectCount(new LambdaQueryWrapper<EquipmentDevice>()
                .eq(EquipmentDevice::getTenantCode, tenantCode)
                .eq(EquipmentDevice::getEquipmentCode, equipmentCode)
                .ne(!creating, EquipmentDevice::getId, request.getId()));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("设备编码已存在");
        }

        entity.setTenantCode(tenantCode);
        entity.setEquipmentCode(equipmentCode);
        entity.setEquipmentName(equipmentName);
        entity.setEquipmentType(cleanText(request.getEquipmentType()));
        entity.setLocation(cleanText(request.getLocation()));
        entity.setResponsiblePerson(cleanText(request.getResponsiblePerson()));
        entity.setInspectionCycleDays(request.getInspectionCycleDays() == null ? 7 : request.getInspectionCycleDays());
        entity.setStatus(normalizeStatus(request.getStatus(), true));
        entity.setRemark(cleanText(request.getRemark()));

        if (entity.getId() == null) {
            equipmentDeviceMapper.insert(entity);
        } else {
            equipmentDeviceMapper.updateById(entity);
        }
        return toDeviceVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("设备ID不能为空");
        }
        int updated = equipmentDeviceMapper.update(null, new LambdaUpdateWrapper<EquipmentDevice>()
                .eq(EquipmentDevice::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(EquipmentDevice::getId, id)
                .set(EquipmentDevice::getStatus, STATUS_DISABLED)
                .set(EquipmentDevice::getUpdateTime, LocalDateTime.now()));
        if (updated <= 0) {
            throw new BusinessException("设备不存在或已停用");
        }
    }

    public Page<EquipmentInspectionRecordVO> recordPage(EquipmentRecordPageRequest request) {
        EquipmentRecordPageRequest safeRequest = request == null ? new EquipmentRecordPageRequest() : request;
        LambdaQueryWrapper<EquipmentInspectionRecord> wrapper = new LambdaQueryWrapper<EquipmentInspectionRecord>()
                .eq(EquipmentInspectionRecord::getTenantCode, TenantPermissionContext.getTenantCode());

        if (safeRequest.getEquipmentId() != null && safeRequest.getEquipmentId() > 0) {
            wrapper.eq(EquipmentInspectionRecord::getEquipmentId, safeRequest.getEquipmentId());
        }
        String equipmentCode = cleanText(safeRequest.getEquipmentCode());
        if (equipmentCode != null) {
            wrapper.eq(EquipmentInspectionRecord::getEquipmentCode, equipmentCode);
        }
        String result = normalizeResult(safeRequest.getResult(), false);
        if (result != null) {
            wrapper.eq(EquipmentInspectionRecord::getInspectionResult, result);
        }
        wrapper.orderByDesc(EquipmentInspectionRecord::getInspectionTime).orderByDesc(EquipmentInspectionRecord::getId);

        Page<EquipmentInspectionRecord> entityPage = inspectionRecordMapper.selectPage(
                new Page<>(safePageNum(safeRequest.getPageNum()), safePageSize(safeRequest.getPageSize())),
                wrapper);
        Page<EquipmentInspectionRecordVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setPages(entityPage.getPages());
        resultPage.setRecords(entityPage.getRecords().stream().map(this::toRecordVO).toList());
        return resultPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public EquipmentInspectionRecordVO submitInspection(EquipmentInspectionSubmitRequest request) {
        EquipmentDevice device = findDeviceByCode(request.getEquipmentCode(), true);
        String result = normalizeResult(request.getInspectionResult(), true);
        String abnormalDesc = cleanText(request.getAbnormalDesc());
        if (RESULT_ABNORMAL.equals(result) && abnormalDesc == null) {
            throw new BusinessException("异常巡检必须填写异常说明");
        }

        LocalDateTime inspectionTime = request.getInspectionTime() == null ? LocalDateTime.now() : request.getInspectionTime();
        EquipmentInspectionRecord record = new EquipmentInspectionRecord();
        record.setTenantCode(device.getTenantCode());
        record.setEquipmentId(device.getId());
        record.setEquipmentCode(device.getEquipmentCode());
        record.setEquipmentName(device.getEquipmentName());
        record.setInspectionResult(result);
        record.setAbnormalDesc(abnormalDesc);
        record.setPhotoUrl(cleanText(request.getPhotoUrl()));
        record.setRemark(cleanText(request.getRemark()));
        record.setInspectorUserId(TenantPermissionContext.getUserId());
        record.setInspectorName(TenantPermissionContext.getUserId() == null ? "系统用户" : "用户" + TenantPermissionContext.getUserId());
        record.setInspectionTime(inspectionTime);
        inspectionRecordMapper.insert(record);

        equipmentDeviceMapper.update(null, new LambdaUpdateWrapper<EquipmentDevice>()
                .eq(EquipmentDevice::getTenantCode, device.getTenantCode())
                .eq(EquipmentDevice::getId, device.getId())
                .set(EquipmentDevice::getLastInspectionTime, inspectionTime)
                .set(EquipmentDevice::getUpdateTime, LocalDateTime.now()));
        return toRecordVO(record);
    }

    private EquipmentDevice findDeviceByCode(String equipmentCode, boolean requireEnabled) {
        String safeCode = cleanText(equipmentCode);
        if (safeCode == null) {
            throw new BusinessException("设备编码不能为空");
        }
        LambdaQueryWrapper<EquipmentDevice> wrapper = new LambdaQueryWrapper<EquipmentDevice>()
                .eq(EquipmentDevice::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(EquipmentDevice::getEquipmentCode, safeCode)
                .last("LIMIT 1");
        if (requireEnabled) {
            wrapper.eq(EquipmentDevice::getStatus, STATUS_ENABLED);
        }
        EquipmentDevice device = equipmentDeviceMapper.selectOne(wrapper);
        if (device == null) {
            throw new BusinessException(requireEnabled ? "设备不存在或已停用" : "设备不存在");
        }
        return device;
    }

    private EquipmentDeviceVO toDeviceVO(EquipmentDevice device) {
        EquipmentDeviceVO vo = new EquipmentDeviceVO();
        BeanUtils.copyProperties(device, vo);
        vo.setInspectionQrPayload(buildInspectionQrPayload(device.getEquipmentCode()));
        return vo;
    }

    private EquipmentInspectionRecordVO toRecordVO(EquipmentInspectionRecord record) {
        EquipmentInspectionRecordVO vo = new EquipmentInspectionRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private String buildInspectionQrPayload(String equipmentCode) {
        String safeCode = equipmentCode == null ? "" : equipmentCode.trim();
        return safeCode.isEmpty() ? "" : "HIVE_EQUIPMENT:" + safeCode;
    }

    private String normalizeStatus(String status, boolean defaultEnabled) {
        String safe = cleanText(status);
        if (safe == null) {
            return defaultEnabled ? STATUS_ENABLED : null;
        }
        if (STATUS_ENABLED.equals(safe) || STATUS_DISABLED.equals(safe)) {
            return safe;
        }
        throw new BusinessException("设备状态不合法");
    }

    private String normalizeResult(String result, boolean required) {
        String safe = cleanText(result);
        if (safe == null) {
            if (required) {
                throw new BusinessException("巡检结果不能为空");
            }
            return null;
        }
        if (RESULT_NORMAL.equals(safe) || RESULT_ABNORMAL.equals(safe)) {
            return safe;
        }
        throw new BusinessException("巡检结果不合法");
    }

    private String cleanText(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value.trim();
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

    private String generateEquipmentCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "EQ" + LocalDateTime.now().format(CODE_TIME_FORMATTER) + suffix;
    }
}
