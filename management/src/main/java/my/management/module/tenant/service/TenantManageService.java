package my.management.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.management.common.exception.BusinessException;
import my.management.module.tenant.mapper.TenantMapper;
import my.management.module.tenant.model.dto.TenantPageRequest;
import my.management.module.tenant.model.entity.Tenant;
import my.management.module.tenant.model.vo.TenantDetailVO;
import my.management.module.tenant.model.vo.TenantPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantManageService {

    @Resource
    private TenantMapper tenantMapper;

    public Page<TenantPageVO> page(TenantPageRequest request) {
        Page<Tenant> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getDeleted, 0)
                .orderByDesc(Tenant::getCreateTime);

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(q -> q.like(Tenant::getTenantName, request.getKeyword().trim())
                    .or()
                    .like(Tenant::getTenantCode, request.getKeyword().trim())
                    .or()
                    .like(Tenant::getContactPerson, request.getKeyword().trim()));
        }
        if (request.getStatus() != null) {
            wrapper.eq(Tenant::getStatus, request.getStatus());
        }

        Page<Tenant> tenantPage = tenantMapper.selectPage(page, wrapper);
        Page<TenantPageVO> result = new Page<>(tenantPage.getCurrent(), tenantPage.getSize(), tenantPage.getTotal());
        List<TenantPageVO> records = tenantPage.getRecords().stream().map(this::toPageVO).toList();
        result.setRecords(records);
        return result;
    }

    public TenantDetailVO detail(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
            throw new BusinessException("租户不存在");
        }
        TenantDetailVO vo = new TenantDetailVO();
        BeanUtils.copyProperties(tenant, vo);
        return vo;
    }

    private TenantPageVO toPageVO(Tenant tenant) {
        TenantPageVO vo = new TenantPageVO();
        BeanUtils.copyProperties(tenant, vo);
        return vo;
    }
}
