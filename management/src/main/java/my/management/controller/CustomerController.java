package my.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import my.management.common.annotation.RequirePermission;
import my.management.common.dto.PageResult;
import my.management.common.dto.Result;
import my.management.module.customer.mapper.CustomerProjectMapper;
import my.management.module.customer.model.dto.CustomerAddRequest;
import my.management.module.customer.model.dto.CustomerPageRequest;
import my.management.module.customer.model.entity.Customer;
import my.management.module.customer.model.entity.CustomerProject;
import my.management.module.customer.model.vo.CustomerDetailVO;
import my.management.module.customer.model.vo.CustomerPageVO;
import my.management.module.customer.service.CustomerService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer")
@Validated
public class CustomerController {

    @Resource
    private CustomerService customerService;

    @Resource
    private CustomerProjectMapper customerProjectMapper;

    @PostMapping("/add")
    @RequirePermission(value = "customer:add", message = "您没有权限新增客户")
    public Result<Void> addCustomer(@Valid @RequestBody CustomerAddRequest request) {
        customerService.addCustomer(request);
        return Result.success(null);
    }

    @GetMapping("/page")
    @RequirePermission(value = "customer:page", message = "您没有权限查看客户列表")
    public Result<PageResult<CustomerPageVO>> getCustomerPage(@Valid CustomerPageRequest request) {
        Page<Customer> page = Optional.ofNullable(customerService.pageSearchCustomer(request)).orElse(new Page<>());

        List<CustomerPageVO> voList = page.getRecords().stream().map(customer -> {
            CustomerPageVO vo = new CustomerPageVO();
            BeanUtils.copyProperties(customer, vo);
            List<CustomerProject> projects = customerProjectMapper.selectList(
                    new LambdaQueryWrapper<CustomerProject>()
                            .eq(CustomerProject::getCustomerId, customer.getId())
                            .orderByDesc(CustomerProject::getId)
            );
            vo.setProjectCount(projects.size());
            vo.setProjectNames(projects.stream().map(CustomerProject::getProjectName).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        PageResult<CustomerPageVO> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setData(voList);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    @RequirePermission(value = "customer:detail", message = "您没有权限查看客户详情")
    public Result<CustomerDetailVO> getCustomer(@PathVariable Long id) {
        return Result.success(customerService.getCustomer(id));
    }
}
