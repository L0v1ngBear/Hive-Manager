package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.module.operationlog.model.dto.OperationLogPageRequest;
import my.management.module.operationlog.model.vo.OperationLogVO;
import my.management.module.operationlog.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台运维日志入口，仅 super 租户可访问。
 */
@RestController
@RequestMapping("/platform/operation-log")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    @GetMapping("/page")
    public Result<PageResult<OperationLogVO>> page(OperationLogPageRequest request) {
        return Result.success(operationLogService.page(request));
    }
}
