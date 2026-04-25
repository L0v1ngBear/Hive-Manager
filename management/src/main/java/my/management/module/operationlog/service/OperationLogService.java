package my.management.module.operationlog.service;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.dto.PageResult;
import my.hive.common.exception.BusinessException;
import my.management.module.operationlog.model.dto.OperationLogPageRequest;
import my.management.module.operationlog.model.vo.OperationLogVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 平台运维日志服务，仅 super 租户可查询。
 */
@Service
public class OperationLogService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public PageResult<OperationLogVO> page(OperationLogPageRequest request) {
        ensureSuper();
        Long current = request.getCurrent() == null || request.getCurrent() < 1 ? 1L : request.getCurrent();
        Long size = request.getSize() == null || request.getSize() < 1 ? 20L : Math.min(request.getSize(), 100L);

        List<Object> params = new ArrayList<>();
        String whereSql = buildWhereSql(request, params);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM operation_log WHERE " + whereSql, Long.class, params.toArray());

        params.add((current - 1) * size);
        params.add(size);
        List<OperationLogVO> records = jdbcTemplate.query("""
                        SELECT
                          id, trace_id AS traceId, tenant_code AS tenantCode, user_id AS userId,
                          module, action, biz_type AS bizType, biz_no AS bizNo, description,
                          log_level AS logLevel, request_method AS requestMethod, request_uri AS requestUri,
                          client_ip AS clientIp, args_json AS argsJson, result_json AS resultJson,
                          success, slow, duration_ms AS durationMs, error_type AS errorType,
                          error_message AS errorMessage, create_time AS createTime
                        FROM operation_log
                        WHERE %s
                        ORDER BY id DESC
                        LIMIT ?, ?
                        """.formatted(whereSql),
                BeanPropertyRowMapper.newInstance(OperationLogVO.class),
                params.toArray());

        PageResult<OperationLogVO> result = new PageResult<>();
        result.setCurrent(current);
        result.setSize(size);
        result.setTotal(total == null ? 0L : total);
        result.setPages((long) Math.ceil(result.getTotal() * 1.0 / size));
        result.setData(records);
        return result;
    }

    private String buildWhereSql(OperationLogPageRequest request, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("1 = 1");
        if (request.getLogLevel() != null && !request.getLogLevel().isBlank()) {
            conditions.add("log_level = ?");
            params.add(request.getLogLevel().trim().toUpperCase());
        }
        if (request.getModule() != null && !request.getModule().isBlank()) {
            conditions.add("module = ?");
            params.add(request.getModule().trim());
        }
        if (request.getSuccess() != null) {
            conditions.add("success = ?");
            params.add(request.getSuccess());
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().trim() + "%";
            conditions.add("(trace_id LIKE ? OR tenant_code LIKE ? OR biz_no LIKE ? OR description LIKE ? OR error_message LIKE ?)");
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }
        return String.join(" AND ", conditions);
    }

    private void ensureSuper() {
        String tenantCode = TenantPermissionContext.getTenantCode();
        if (!"super".equalsIgnoreCase(tenantCode)) {
            throw new BusinessException(403, "仅平台超管可查看运维日志");
        }
    }
}
