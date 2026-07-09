package my.management.module.manual.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.manual.mapper.TenantManualMapper;
import my.management.module.manual.model.dto.TenantManualSaveRequest;
import my.management.module.manual.model.entity.TenantManual;
import my.management.module.manual.model.vo.TenantManualVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

@Service
public class TenantManualService {

    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private TenantManualMapper tenantManualMapper;

    public TenantManualVO current() {
        TenantManual manual = tenantManualMapper.selectByTenantCode(requireTenantCode());
        return toVO(manual);
    }

    public TenantManualVO save(TenantManualSaveRequest request) {
        String tenantCode = requireTenantCode();
        String content = request == null || request.getContent() == null ? "" : request.getContent().trim();
        tenantManualMapper.upsert(tenantCode, content, TenantPermissionContext.getUserId());
        return current();
    }

    private String requireTenantCode() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("当前组织信息缺失，请重新登录");
        }
        return tenantCode;
    }

    private TenantManualVO toVO(TenantManual manual) {
        TenantManualVO vo = new TenantManualVO();
        if (manual == null) {
            vo.setContent("");
            vo.setSavedAt("");
            return vo;
        }
        vo.setContent(manual.getContent() == null ? "" : manual.getContent());
        vo.setUpdaterId(manual.getUpdaterId());
        vo.setSavedAt(manual.getUpdateTime() == null ? "" : DISPLAY_TIME_FORMATTER.format(manual.getUpdateTime()));
        return vo;
    }
}
