package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.vo.ImportResultVO;
import my.management.module.price.model.dto.PricePageRequest;
import my.management.module.price.model.dto.PricePublishRequest;
import my.management.module.price.model.vo.CustomerOptionVO;
import my.management.module.price.model.vo.ModelSpecOptionVO;
import my.management.module.price.model.vo.PriceDetailVO;
import my.management.module.price.model.vo.PriceSkuVO;
import my.management.module.price.model.vo.PriceStatsVO;
import my.management.module.price.service.PriceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
/**
 * PriceController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/price")
@Validated
public class PriceController {

    @Resource
    private PriceService priceService;

    @GetMapping("/page")
    @RequirePermission(value = "price:list", message = "您没有权限查看价格列表")
    public Result<PageResult<PriceSkuVO>> page(@Valid PricePageRequest request) {
        Page<PriceSkuVO> page = priceService.page(request);
        PageResult<PriceSkuVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(page.getRecords());
        return Result.success(result);
    }

    @GetMapping("/stats")
    @RequirePermission(value = "price:list", message = "您没有权限查看价格统计")
    public Result<PriceStatsVO> stats() {
        return Result.success(priceService.stats());
    }

    @PostMapping("/publish")
    @RequirePermission(value = "price:publish", message = "您没有权限发布价格")
    public Result<Long> publish(@Valid @RequestBody PricePublishRequest request) {
        return Result.success(priceService.publish(request));
    }

    @GetMapping("/detail/{id}")
    @RequirePermission(value = "price:detail", message = "您没有权限查看价格详情")
    public Result<PriceDetailVO> detail(@PathVariable Long id) {
        return Result.success(priceService.detail(id));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "price:delete", message = "您没有权限删除价格")
    public Result<Void> delete(@PathVariable Long id) {
        priceService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/customers")
    @RequirePermission(value = "price:list", message = "您没有权限查看客户选项")
    public Result<List<CustomerOptionVO>> customers(String keyword) {
        return Result.success(priceService.customerOptions(keyword));
    }

    @GetMapping("/models")
    @RequirePermission(value = "price:list", message = "您没有权限查看型号选项")
    public Result<List<ModelSpecOptionVO>> models(String keyword) {
        return Result.success(priceService.modelOptions(keyword));
    }

    @GetMapping("/export-excel")
    @RequirePermission(value = "price:list", message = "您没有权限导出价格数据")
    public void exportExcel(@Valid PricePageRequest request, HttpServletResponse response) {
        priceService.exportExcel(request, response);
    }

    @GetMapping("/import-template")
    @RequirePermission(value = "price:list", message = "您没有权限下载价格导入模板")
    public void downloadImportTemplate(HttpServletResponse response) {
        priceService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    @RequirePermission(value = "price:publish", message = "您没有权限导入价格数据")
    public Result<ImportResultVO> importPrices(@RequestParam("file") MultipartFile file) {
        return Result.success(priceService.importPrices(file));
    }
}
