package my.management.module.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.customer.mapper.CustomerContactMapper;
import my.management.module.customer.mapper.CustomerMapper;
import my.management.module.customer.mapper.CustomerProjectMapper;
import my.management.module.customer.model.dto.CustomerAddRequest;
import my.management.module.customer.model.dto.CustomerPageRequest;
import my.management.module.customer.model.dto.CustomerUpdateRequest;
import my.management.module.customer.model.entity.Customer;
import my.management.module.customer.model.entity.CustomerContact;
import my.management.module.customer.model.entity.CustomerProject;
import my.management.module.customer.model.vo.CustomerDetailVO;
import my.management.module.customer.model.vo.CustomerOptionVO;
import my.management.module.customer.model.vo.CustomerPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
/**
 * CustomerService 属于管理端后端客户模块，实现核心业务编排与规则逻辑。
 */
@Service
public class CustomerService {

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private CustomerContactMapper customerContactMapper;

    @Resource
    private CustomerProjectMapper customerProjectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void addCustomer(CustomerAddRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();

        Long count = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerName, request.getCustomerName())
                .eq(Customer::getTenantCode, tenantCode));
        if (count > 0) {
            throw new BusinessException("customer already exists");
        }

        Customer customer = new Customer();
        customer.setCustomerName(request.getCustomerName());
        customer.setCustomerType(request.getCustomerType());
        customer.setTenantCode(tenantCode);
        customerMapper.insert(customer);
        saveContactsAndProjects(tenantCode, customer.getId(), request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(CustomerUpdateRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode)
                .eq(Customer::getId, request.getId())
                .last("LIMIT 1"));
        if (customer == null) {
            throw new BusinessException("customer not found");
        }

        Long duplicateCount = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode)
                .eq(Customer::getCustomerName, request.getCustomerName())
                .ne(Customer::getId, request.getId()));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("customer already exists");
        }

        customer.setCustomerName(request.getCustomerName());
        customer.setCustomerType(request.getCustomerType());
        customerMapper.updateById(customer);

        // Replace children in one transaction so the drawer can submit the full latest state directly.
        customerContactMapper.delete(new LambdaQueryWrapper<CustomerContact>()
                .eq(CustomerContact::getTenantCode, tenantCode)
                .eq(CustomerContact::getCustomerId, request.getId()));
        customerProjectMapper.delete(new LambdaQueryWrapper<CustomerProject>()
                .eq(CustomerProject::getTenantCode, tenantCode)
                .eq(CustomerProject::getCustomerId, request.getId()));

        saveContactsAndProjects(tenantCode, request.getId(), request);
    }

    public Page<CustomerPageVO> pageSearchCustomer(CustomerPageRequest request) {
        String keyword = request.getKeyword();
        String tenantCode = TenantPermissionContext.getTenantCode();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getTenantCode, tenantCode);

        if (StringUtils.isNotBlank(keyword)) {
            String safeKeyword = keyword.replace("'", "''");
            wrapper.and(w -> w
                    .like(Customer::getCustomerName, safeKeyword)
                    .or().inSql(Customer::getId,
                            "SELECT customer_id FROM customer_project WHERE tenant_code = '" + tenantCode + "' AND project_name LIKE '%" + safeKeyword + "%'")
                    .or().inSql(Customer::getId,
                            "SELECT customer_id FROM customer_contact WHERE tenant_code = '" + tenantCode + "' AND (contact_name LIKE '%" + safeKeyword + "%' OR contact_phone LIKE '%" + safeKeyword + "%')")
            );
        }

        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Customer> customerPage = customerMapper.selectPage(page, wrapper);

        // Batch-enrich the current page to avoid per-row contact/project queries.
        List<CustomerPageVO> records = enrichCustomerPage(customerPage.getRecords(), tenantCode);
        Page<CustomerPageVO> result = new Page<>(customerPage.getCurrent(), customerPage.getSize(), customerPage.getTotal());
        result.setPages(customerPage.getPages());
        result.setRecords(records);
        return result;
    }

    public CustomerDetailVO getCustomer(Long id) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode)
                .eq(Customer::getId, id)
                .last("LIMIT 1"));
        if (customer == null) {
            throw new BusinessException("customer not found");
        }

        List<CustomerContact> customerContactList = customerContactMapper.selectList(new LambdaQueryWrapper<CustomerContact>()
                .eq(CustomerContact::getTenantCode, tenantCode)
                .eq(CustomerContact::getCustomerId, id));
        List<CustomerProject> customerProjectList = customerProjectMapper.selectList(new LambdaQueryWrapper<CustomerProject>()
                .eq(CustomerProject::getTenantCode, tenantCode)
                .eq(CustomerProject::getCustomerId, id));

        CustomerDetailVO customerDetailVO = new CustomerDetailVO();
        BeanUtils.copyProperties(customer, customerDetailVO);
        customerDetailVO.setContacts(customerContactList);
        customerDetailVO.setProjects(customerProjectList);
        return customerDetailVO;
    }

    public List<CustomerOptionVO> listCustomerOptions(String keyword) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode)
                .orderByDesc(Customer::getId);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Customer::getCustomerName, keyword.trim());
        }
        List<Customer> customers = customerMapper.selectList(wrapper);
        if (customers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> customerIds = customers.stream().map(Customer::getId).toList();
        Map<Long, List<String>> projectNamesByCustomerId = customerProjectMapper.selectList(new LambdaQueryWrapper<CustomerProject>()
                        .eq(CustomerProject::getTenantCode, tenantCode)
                        .in(CustomerProject::getCustomerId, customerIds)
                        .orderByDesc(CustomerProject::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        CustomerProject::getCustomerId,
                        LinkedHashMap::new,
                        Collectors.mapping(CustomerProject::getProjectName, Collectors.toList())
                ));

        return customers.stream().map(customer -> {
            CustomerOptionVO vo = new CustomerOptionVO();
            vo.setId(customer.getId());
            vo.setCustomerName(customer.getCustomerName());
            vo.setProjectNames(projectNamesByCustomerId.getOrDefault(customer.getId(), Collections.emptyList()));
            return vo;
        }).toList();
    }

    private void saveContactsAndProjects(String tenantCode, Long customerId, CustomerAddRequest request) {
        if (request.getContacts() != null) {
            for (CustomerContact contactDto : request.getContacts()) {
                if (!StringUtils.isNotBlank(contactDto.getContactName()) && !StringUtils.isNotBlank(contactDto.getContactPhone())) {
                    continue;
                }
                CustomerContact contact = new CustomerContact();
                contact.setTenantCode(tenantCode);
                contact.setCustomerId(customerId);
                contact.setContactName(contactDto.getContactName());
                contact.setContactPhone(contactDto.getContactPhone());
                customerContactMapper.insert(contact);
            }
        }

        if (request.getProjects() != null) {
            for (CustomerProject projectDto : request.getProjects()) {
                if (!StringUtils.isNotBlank(projectDto.getProjectName())) {
                    continue;
                }
                CustomerProject project = new CustomerProject();
                project.setTenantCode(tenantCode);
                project.setCustomerId(customerId);
                project.setProjectName(projectDto.getProjectName().trim());
                project.setConstructionArea(StringUtils.isNotBlank(projectDto.getConstructionArea())
                        ? projectDto.getConstructionArea().trim()
                        : null);
                customerProjectMapper.insert(project);
            }
        }
    }

    private List<CustomerPageVO> enrichCustomerPage(List<Customer> customers, String tenantCode) {
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> customerIds = customers.stream()
                .map(Customer::getId)
                .filter(Objects::nonNull)
                .toList();

        // Keep contacts/projects grouped in memory so the page endpoint stays O(1) database round-trips.
        Map<Long, List<CustomerContact>> contactsByCustomerId = customerContactMapper.selectList(
                        new LambdaQueryWrapper<CustomerContact>()
                                .eq(CustomerContact::getTenantCode, tenantCode)
                                .in(CustomerContact::getCustomerId, customerIds)
                                .orderByDesc(CustomerContact::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        CustomerContact::getCustomerId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Long, List<CustomerProject>> projectsByCustomerId = customerProjectMapper.selectList(
                        new LambdaQueryWrapper<CustomerProject>()
                                .eq(CustomerProject::getTenantCode, tenantCode)
                                .in(CustomerProject::getCustomerId, customerIds)
                                .orderByDesc(CustomerProject::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        CustomerProject::getCustomerId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return customers.stream().map(customer -> {
            CustomerPageVO vo = new CustomerPageVO();
            BeanUtils.copyProperties(customer, vo);

            List<CustomerContact> contacts = contactsByCustomerId.getOrDefault(customer.getId(), Collections.emptyList());
            List<CustomerProject> projects = projectsByCustomerId.getOrDefault(customer.getId(), Collections.emptyList());

            vo.setContacts(contacts);
            vo.setProjects(projects);
            vo.setProjectCount(projects.size());
            vo.setProjectNames(projects.stream().map(CustomerProject::getProjectName).toList());
            return vo;
        }).toList();
    }
}
