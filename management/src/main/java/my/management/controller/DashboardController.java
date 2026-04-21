package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.dto.Result;
import my.management.module.ai.model.vo.DashboardAiAdviceVO;
import my.management.module.dashboard.model.vo.DashboardOverviewVO;
import my.management.module.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }

    @GetMapping("/ai-advices")
    public Result<List<DashboardAiAdviceVO>> aiAdvices() {
        return Result.success(dashboardService.aiAdvices());
    }
}
