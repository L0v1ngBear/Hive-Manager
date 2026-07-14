package my.hive.api.print;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.hive.domain.print.PrintTaskService;
import my.hive.shared.annotation.CollectLog;
import my.hive.shared.dto.Result;
import my.hive.domain.print.dto.PrintTaskReportRequest;
import my.hive.domain.print.vo.PrintTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 打印任务通用接口。
 * 两套后端统一使用该入口，避免各业务端重复声明同路径 Controller 导致启动冲突。
 */
@RestController
@RequestMapping("/print-task")
@RequiredArgsConstructor
public class PrintTaskController {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 100;

    private final PrintTaskService printTaskService;

    @PostMapping("/report")
    @CollectLog(module = "print_task", action = "report", bizType = "print_task", bizNo = "#request.taskNo", description = "Print task result report")
    public Result<Void> report(@Valid @RequestBody PrintTaskReportRequest request) {
        printTaskService.report(request);
        return Result.success(null);
    }

    @GetMapping("/recent")
    public Result<List<PrintTaskVO>> recent(@RequestParam(required = false, defaultValue = "label") String printType,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(printTaskService.recent(printType, normalizeLimit(limit)));
    }

    @GetMapping("/pending")
    public Result<List<PrintTaskVO>> pending(@RequestParam(required = false, defaultValue = "label") String printType,
                                             @RequestParam(required = false) Integer limit) {
        return Result.success(printTaskService.pending(printType, normalizeLimit(limit)));
    }

    @GetMapping("/pending-count")
    public Result<Map<String, Long>> pendingCount(@RequestParam(required = false) String printTypes) {
        List<String> types = printTypes == null || printTypes.isBlank()
                ? List.of()
                : Arrays.stream(printTypes.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
        return Result.success(printTaskService.pendingCount(types));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

}
