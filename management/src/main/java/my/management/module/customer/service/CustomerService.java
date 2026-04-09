package my.management.module.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import my.management.common.context.TenantPermissionContext;
import my.management.common.exception.BusinessException;
import my.management.module.customer.mapper.CustomerContactMapper;
import my.management.module.customer.mapper.CustomerMapper;
import my.management.module.customer.mapper.CustomerProjectMapper;
import my.management.module.customer.model.dto.CustomerAddRequest;
import my.management.module.customer.model.dto.CustomerPageRequest;
import my.management.module.customer.model.entity.Customer;
import my.management.module.customer.model.entity.CustomerContact;
import my.management.module.customer.model.entity.CustomerProject;
import my.management.module.customer.model.vo.CustomerDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        customer.setConstructionArea(request.getConstructionArea());
        customer.setTenantCode(tenantCode);
        customerMapper.insert(customer);

        Long customerId = customer.getId();

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
                project.setProjectName(projectDto.getProjectName());
                customerProjectMapper.insert(project);
            }
        }
    }

    public Page<Customer> pageSearchCustomer(CustomerPageRequest request) {
        String keyword = request.getKeyword();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getTenantCode, TenantPermissionContext.getTenantCode());

        if (StringUtils.isNotBlank(keyword)) {
            String safeKeyword = keyword.replace("'", "''");
            wrapper.and(w -> w
                    .like(Customer::getCustomerName, safeKeyword)
                    .or().inSql(Customer::getId,
                            "SELECT customer_id FROM customer_project WHERE project_name LIKE '%" + safeKeyword + "%'")
                    .or().inSql(Customer::getId,
                            "SELECT customer_id FROM customer_contact WHERE (contact_name LIKE '%" + safeKeyword + "%' OR contact_phone LIKE '%" + safeKeyword + "%')")
            );
        }

        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> page = new Page<>(request.getPageNum(), request.getPageSize());
        return customerMapper.selectPage(page, wrapper);
    }

    public CustomerDetailVO getCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("customer not found");
        }

        List<CustomerContact> customerContactList = customerContactMapper.selectList(new LambdaQueryWrapper<CustomerContact>()
                .eq(CustomerContact::getCustomerId, id));
        List<CustomerProject> customerProjectList = customerProjectMapper.selectList(new LambdaQueryWrapper<CustomerProject>()
                .eq(CustomerProject::getCustomerId, id));

        CustomerDetailVO customerDetailVO = new CustomerDetailVO();
        BeanUtils.copyProperties(customer, customerDetailVO);
        customerDetailVO.setContacts(customerContactList);
        customerDetailVO.setProjects(customerProjectList);
        return customerDetailVO;
    }
}
