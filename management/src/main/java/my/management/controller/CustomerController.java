package my.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.hive.common.annotation.CollectLog;
import my.hive.common.annotation.RequirePermission;
import my.management.module.sys.model.enums.PermissionCodeEnum;
import my.hive.common.dto.PageResult;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.tenant.model.enums.TenantFeatureEnum;
import my.management.module.customer.model.dto.CustomerAddRequest;
import my.management.module.customer.model.dto.CustomerPageRequest;
import my.management.module.customer.model.dto.CustomerUpdateRequest;
import my.management.module.customer.model.vo.CustomerDetailVO;
import my.management.module.customer.model.vo.CustomerOptionVO;
import my.management.module.customer.model.vo.CustomerPageVO;
import my.management.module.customer.service.CustomerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.List;
/**
 * CustomerController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
@RestController
@RequestMapping("/customer")
@RequireTenantFeature(TenantFeatureEnum.CODE_CUSTOMER)
@Validated
public class CustomerController {

    @Resource
    private CustomerService customerService;

    @PostMapping("/add")
    @RequirePermission(value = PermissionCodeEnum.CODE_CUSTOMER_CREATE, message = "您没有权限新增客户")
    @CollectLog(module = "customer", action = "create", bizType = "customer", bizNo = "#request.customerName", description = "管理端新增客户")
    public Result<Void> addCustomer(@Valid @RequestBody CustomerAddRequest request) {
        customerService.addCustomer(request);
        return Result.success(null);
    }

    @PostMapping("/update")
    @RequirePermission(value = PermissionCodeEnum.CODE_CUSTOMER_UPDATE, message = "您没有权限编辑客户")
    @CollectLog(module = "customer", action = "update", bizType = "customer", bizNo = "#request.id", description = "管理端编辑客户")
    public Result<Void> updateCustomer(@Valid @RequestBody CustomerUpdateRequest request) {
        customerService.updateCustomer(request);
        return Result.success(null);
    }

    @GetMapping("/page")
    @RequirePermission(value = PermissionCodeEnum.CODE_CUSTOMER_LIST, message = "您没有权限查看客户列表")
    public Result<PageResult<CustomerPageVO>> getCustomerPage(@Valid CustomerPageRequest request) {
        Page<CustomerPageVO> page = Optional.ofNullable(customerService.pageSearchCustomer(request)).orElse(new Page<>());

        PageResult<CustomerPageVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(page.getRecords());
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    @RequirePermission(value = PermissionCodeEnum.CODE_CUSTOMER_DETAIL, message = "您没有权限查看客户详情")
    public Result<CustomerDetailVO> getCustomer(@PathVariable Long id) {
        return Result.success(customerService.getCustomer(id));
    }

    @GetMapping("/options")
    @RequirePermission(value = PermissionCodeEnum.CODE_CUSTOMER_LIST, message = "您没有权限查看客户选项")
    public Result<List<CustomerOptionVO>> listCustomerOptions(String keyword) {
        return Result.success(customerService.listCustomerOptions(keyword));
    }
}
