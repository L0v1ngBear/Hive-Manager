package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.Result;
import my.management.module.ai.model.dto.AiAdviceFeedbackRequest;
import my.management.module.ai.model.vo.AiBusinessSnapshotVO;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.ai.service.AiAdviceFeedbackService;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import my.management.module.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * DashboardController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Resource
    private AiAdviceFeedbackService aiAdviceFeedbackService;

    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }

    @GetMapping("/ai-advices")
    public Result<List<DashboardAiAdviceVO>> aiAdvices(@RequestParam(defaultValue = "false") Boolean refresh) {
        return Result.success(dashboardService.aiAdvices(Boolean.TRUE.equals(refresh)));
    }

    @GetMapping("/ai-snapshot")
    @RequirePermission(value = "dashboard:ai:view", message = "您没有权限查看 AI 经营快照")
    public Result<AiBusinessSnapshotVO> aiSnapshot() {
        return Result.success(dashboardService.aiSnapshot());
    }

    @PostMapping("/ai-advices/feedback")
    public Result<Void> aiAdviceFeedback(@RequestBody AiAdviceFeedbackRequest request) {
        aiAdviceFeedbackService.feedback(request);
        return Result.success(null);
    }
}
