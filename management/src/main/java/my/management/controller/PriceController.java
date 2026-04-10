package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.dto.PageResult;
import my.management.common.dto.Result;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/price")
@Validated
public class PriceController {

    @Resource
    private PriceService priceService;

    @GetMapping("/page")
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
    public Result<PriceStatsVO> stats() {
        return Result.success(priceService.stats());
    }

    @PostMapping("/publish")
    public Result<Long> publish(@Valid @RequestBody PricePublishRequest request) {
        return Result.success(priceService.publish(request));
    }

    @GetMapping("/detail/{id}")
    public Result<PriceDetailVO> detail(@PathVariable Long id) {
        return Result.success(priceService.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        priceService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/customers")
    public Result<List<CustomerOptionVO>> customers(String keyword) {
        return Result.success(priceService.customerOptions(keyword));
    }

    @GetMapping("/models")
    public Result<List<ModelSpecOptionVO>> models(String keyword) {
        return Result.success(priceService.modelOptions(keyword));
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(priceService.categories());
    }
}