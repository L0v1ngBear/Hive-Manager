package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.module.systemevent.model.dto.SystemEventPageRequest;
import my.management.module.systemevent.model.vo.SystemEventVO;
import my.management.module.systemevent.service.SystemEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/system-event")
public class SystemEventController {

    @Resource
    private SystemEventService systemEventService;

    @GetMapping("/page")
    public Result<PageResult<SystemEventVO>> page(SystemEventPageRequest request) {
        return Result.success(systemEventService.page(request));
    }

    @PostMapping("/{id}/handle")
    public Result<Void> markHandled(@PathVariable Long id) {
        systemEventService.markHandled(id);
        return Result.success(null);
    }
}
