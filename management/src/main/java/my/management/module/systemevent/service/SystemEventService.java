package my.management.module.systemevent.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.management.common.enums.PlatformTenantEnum;
import my.management.module.systemevent.model.dto.SystemEventPageRequest;
import my.management.module.systemevent.model.vo.SystemEventVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SystemEventService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public PageResult<SystemEventVO> page(SystemEventPageRequest request) {
        ensureSuper();
        ensureTableReady();

        SystemEventPageRequest safeRequest = request == null ? new SystemEventPageRequest() : request;
        long current = safeRequest.getCurrent() == null || safeRequest.getCurrent() < 1 ? 1L : safeRequest.getCurrent();
        long size = safeRequest.getSize() == null || safeRequest.getSize() < 1 ? 20L : Math.min(safeRequest.getSize(), 100L);

        List<Object> params = new ArrayList<>();
        String whereSql = buildWhereSql(safeRequest, params);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM system_event WHERE " + whereSql,
                Long.class, params.toArray());

        params.add((current - 1) * size);
        params.add(size);
        List<SystemEventVO> records = jdbcTemplate.query("""
                        SELECT
                          id, event_key AS eventKey, source_app AS sourceApp, event_type AS eventType,
                          level, tenant_code AS tenantCode, module, title, content, biz_type AS bizType,
                          biz_no AS bizNo, trace_id AS traceId, detail_json AS detailJson,
                          handled, handled_by AS handledBy, handled_time AS handledTime, create_time AS createTime
                        FROM system_event
                        WHERE %s
                        ORDER BY id DESC
                        LIMIT ?, ?
                        """.formatted(whereSql),
                BeanPropertyRowMapper.newInstance(SystemEventVO.class),
                params.toArray());

        PageResult<SystemEventVO> result = new PageResult<>();
        result.setCurrent(current);
        result.setSize(size);
        result.setTotal(total == null ? 0L : total);
        result.setPages((long) Math.ceil(result.getTotal() * 1.0 / size));
        result.setData(records);
        return result;
    }

    public void markHandled(Long id) {
        ensureSuper();
        ensureTableReady();
        if (id == null || id <= 0) {
            throw new BusinessException(400, "事件ID不合法");
        }
        String handler = TenantPermissionContext.getUserId() == null
                ? PlatformTenantEnum.SUPER.getCode()
                : String.valueOf(TenantPermissionContext.getUserId());
        jdbcTemplate.update("""
                UPDATE system_event
                SET handled = 1, handled_by = ?, handled_time = NOW()
                WHERE id = ? AND handled = 0
                """, handler, id);
    }

    private String buildWhereSql(SystemEventPageRequest request, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("1 = 1");
        addEqualsCondition(conditions, params, "level", request.getLevel(), true);
        addEqualsCondition(conditions, params, "source_app", request.getSourceApp(), false);
        addEqualsCondition(conditions, params, "event_type", request.getEventType(), false);
        addEqualsCondition(conditions, params, "module", request.getModule(), false);
        addEqualsCondition(conditions, params, "tenant_code", request.getTenantCode(), false);
        if (request.getHandled() != null) {
            conditions.add("handled = ?");
            params.add(request.getHandled() == 1 ? 1 : 0);
        }
        if (request.getStartTime() != null) {
            conditions.add("create_time >= ?");
            params.add(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            conditions.add("create_time <= ?");
            params.add(request.getEndTime());
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().trim() + "%";
            conditions.add("""
                    (event_key LIKE ? OR tenant_code LIKE ? OR module LIKE ? OR title LIKE ?
                     OR content LIKE ? OR biz_no LIKE ? OR trace_id LIKE ?)
                    """);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }
        return String.join(" AND ", conditions);
    }

    private void addEqualsCondition(List<String> conditions, List<Object> params, String column, String value, boolean upperCase) {
        if (value == null || value.isBlank()) {
            return;
        }
        conditions.add(column + " = ?");
        String normalized = value.trim();
        params.add(upperCase ? normalized.toUpperCase() : normalized);
    }

    private void ensureTableReady() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'system_event'
                """, Integer.class);
        if (count == null || count == 0) {
            throw new BusinessException(500, "system_event表不存在，请先执行线上数据库迁移");
        }
    }

    private void ensureSuper() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (!PlatformTenantEnum.isSuper(tenantCode)) {
            throw new BusinessException(403, "仅平台超管可查看系统重要事件");
        }
    }
}
