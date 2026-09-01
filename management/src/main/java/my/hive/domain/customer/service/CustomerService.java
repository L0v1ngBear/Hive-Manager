package my.hive.domain.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.dto.ImportResultVO;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.utils.ExcelUtil;
import my.hive.domain.customer.mapper.CustomerContactMapper;
import my.hive.domain.customer.mapper.CustomerMapper;
import my.hive.domain.customer.mapper.CustomerProjectMapper;
import my.hive.domain.customer.model.dto.CustomerAddRequest;
import my.hive.domain.customer.model.dto.CustomerPageRequest;
import my.hive.domain.customer.model.dto.CustomerUpdateRequest;
import my.hive.domain.customer.model.entity.Customer;
import my.hive.domain.customer.model.entity.CustomerContact;
import my.hive.domain.customer.model.entity.CustomerProject;
import my.hive.domain.customer.model.enums.CustomerTypeEnum;
import my.hive.domain.customer.model.vo.CustomerDetailVO;
import my.hive.domain.customer.model.vo.CustomerOptionVO;
import my.hive.domain.customer.model.vo.CustomerPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * CustomerService 属于管理端后端客户模块，实现核心业务编排与规则逻辑。
 */
@Service
public class CustomerService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int CUSTOMER_IMPORT_COLUMN_COUNT = 7;
    private static final int MAX_CUSTOMER_IMPORT_ROWS = 2000;
    private static final long MAX_IMPORT_FILE_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final List<String> CUSTOMER_IMPORT_HEADERS = List.of(
            "客户名称", "客户类型", "联系人", "联系电话", "项目名称", "施工区域", "项目负责人");

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private CustomerContactMapper customerContactMapper;

    @Resource
    private CustomerProjectMapper customerProjectMapper;

    @Resource
    private ExcelUtil excelUtil;

    @Transactional(rollbackFor = Exception.class)
    public void addCustomer(CustomerAddRequest request) {
        String tenantCode = TenantPermissionContext.getTenantCode();

        Customer existing = customerMapper.selectByTenantCodeAndNameForUpdate(tenantCode, request.getCustomerName());
        if (existing != null) {
            throw new BusinessException("客户已存在");
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
            throw new BusinessException("客户不存在");
        }

        Customer duplicate = customerMapper.selectByTenantCodeAndNameForUpdate(tenantCode, request.getCustomerName());
        if (duplicate != null && !Objects.equals(duplicate.getId(), request.getId())) {
            throw new BusinessException("客户已存在");
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
        if (request == null) {
            request = new CustomerPageRequest();
        }
        String keyword = request.getKeyword();
        String tenantCode = TenantPermissionContext.getTenantCode();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode);

        if (StringUtils.isNotBlank(keyword)) {
            String safeKeyword = keyword.trim();
            wrapper.and(w -> w
                    .like(Customer::getCustomerName, safeKeyword)
                    .or().apply("id IN (SELECT customer_id FROM customer_project WHERE tenant_code = {1} AND (project_name LIKE CONCAT('%', {0}, '%') OR project_owner LIKE CONCAT('%', {0}, '%')))", safeKeyword, tenantCode)
                    .or().apply("id IN (SELECT customer_id FROM customer_contact WHERE tenant_code = {1} AND (contact_name LIKE CONCAT('%', {0}, '%') OR contact_phone LIKE CONCAT('%', {0}, '%')))", safeKeyword, tenantCode)
            );
        }
        if (request.getCustomerType() != null) {
            wrapper.eq(Customer::getCustomerType, request.getCustomerType());
        }
        if (request.getCreateStart() != null) {
            wrapper.ge(Customer::getCreateTime, request.getCreateStart().atStartOfDay());
        }
        if (request.getCreateEnd() != null) {
            wrapper.lt(Customer::getCreateTime, request.getCreateEnd().plusDays(1).atStartOfDay());
        }

        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> page = new Page<>(safePageNum(request.getPageNum()), safePageSize(request.getPageSize()));
        Page<Customer> customerPage = customerMapper.selectPage(page, wrapper);

        // Batch-enrich the current page to avoid per-row contact/project queries.
        List<CustomerPageVO> records = enrichCustomerPage(customerPage.getRecords(), tenantCode);
        Page<CustomerPageVO> result = new Page<>(customerPage.getCurrent(), customerPage.getSize(), customerPage.getTotal());
        result.setPages(customerPage.getPages());
        result.setRecords(records);
        return result;
    }

    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public CustomerDetailVO getCustomer(Long id) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantCode, tenantCode)
                .eq(Customer::getId, id)
                .last("LIMIT 1"));
        if (customer == null) {
            throw new BusinessException("客户不存在");
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
            String safeKeyword = keyword.trim();
            wrapper.and(w -> w
                    .like(Customer::getCustomerName, safeKeyword)
                    .or().apply("id IN (SELECT customer_id FROM customer_project WHERE tenant_code = {1} AND (project_name LIKE CONCAT('%', {0}, '%') OR project_owner LIKE CONCAT('%', {0}, '%')))", safeKeyword, tenantCode)
                    .or().apply("id IN (SELECT customer_id FROM customer_contact WHERE tenant_code = {1} AND (contact_name LIKE CONCAT('%', {0}, '%') OR contact_phone LIKE CONCAT('%', {0}, '%')))", safeKeyword, tenantCode)
            );
        }
        wrapper.last("LIMIT 30");
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
        Map<Long, CustomerContact> contactByCustomerId = customerContactMapper.selectList(new LambdaQueryWrapper<CustomerContact>()
                        .eq(CustomerContact::getTenantCode, tenantCode)
                        .in(CustomerContact::getCustomerId, customerIds)
                        .orderByDesc(CustomerContact::getId))
                .stream()
                .collect(Collectors.toMap(
                        CustomerContact::getCustomerId,
                        contact -> contact,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));

        return customers.stream().map(customer -> {
            CustomerOptionVO vo = new CustomerOptionVO();
            vo.setId(customer.getId());
            vo.setCustomerName(customer.getCustomerName());
            CustomerContact contact = contactByCustomerId.get(customer.getId());
            if (contact != null) {
                vo.setContactName(contact.getContactName());
                vo.setContactPhone(contact.getContactPhone());
            }
            vo.setProjectNames(projectNamesByCustomerId.getOrDefault(customer.getId(), Collections.emptyList()));
            return vo;
        }).toList();
    }

    /**
     * An orderless after-sales ticket still belongs in the shared customer catalog.
     * Reuse an exact tenant customer name and append only missing contact/project
     * children so existing customer-management data is never replaced.
     */
    @Transactional(rollbackFor = Exception.class)
    public Customer ensureAfterSalesCustomer(String customerName,
                                             String contactName,
                                             String contactPhone,
                                             String projectName) {
        String tenantCode = TenantPermissionContext.getTenantCode();
        String normalizedCustomerName = normalizeAfterSalesText(customerName, 120, "请填写客户名称", "客户名称不能超过120个字符");
        String normalizedContactName = normalizeOptionalAfterSalesText(contactName, 64, "联系人不能超过64个字符");
        String normalizedContactPhone = normalizeOptionalAfterSalesText(contactPhone, 32, "联系电话不能超过32个字符");
        String normalizedProjectName = normalizeOptionalAfterSalesText(projectName, 128, "项目名称不能超过128个字符");

        Customer customer = customerMapper.selectByTenantCodeAndNameForUpdate(tenantCode, normalizedCustomerName);
        if (customer == null) {
            customer = new Customer();
            customer.setTenantCode(tenantCode);
            customer.setCustomerName(normalizedCustomerName);
            customer.setCustomerType(CustomerTypeEnum.DEFAULT.getCode());
            customerMapper.insert(customer);
        }

        if (normalizedContactName != null || normalizedContactPhone != null) {
            String storedContactName = normalizedContactName == null ? normalizedCustomerName : normalizedContactName;
            LambdaQueryWrapper<CustomerContact> contactQuery = new LambdaQueryWrapper<CustomerContact>()
                    .eq(CustomerContact::getTenantCode, tenantCode)
                    .eq(CustomerContact::getCustomerId, customer.getId())
                    .eq(CustomerContact::getContactName, storedContactName);
            if (normalizedContactPhone == null) contactQuery.isNull(CustomerContact::getContactPhone);
            else contactQuery.eq(CustomerContact::getContactPhone, normalizedContactPhone);
            if (customerContactMapper.selectCount(contactQuery) == 0) {
                CustomerContact contact = new CustomerContact();
                contact.setTenantCode(tenantCode);
                contact.setCustomerId(customer.getId());
                contact.setContactName(storedContactName);
                contact.setContactPhone(normalizedContactPhone);
                customerContactMapper.insert(contact);
            }
        }

        if (normalizedProjectName != null) {
            long projectCount = customerProjectMapper.selectCount(new LambdaQueryWrapper<CustomerProject>()
                    .eq(CustomerProject::getTenantCode, tenantCode)
                    .eq(CustomerProject::getCustomerId, customer.getId())
                    .eq(CustomerProject::getProjectName, normalizedProjectName));
            if (projectCount == 0) {
                CustomerProject project = new CustomerProject();
                project.setTenantCode(tenantCode);
                project.setCustomerId(customer.getId());
                project.setProjectName(normalizedProjectName);
                customerProjectMapper.insert(project);
            }
        }
        return customer;
    }

    private String normalizeAfterSalesText(String value, int maxLength, String emptyMessage, String lengthMessage) {
        String normalized = normalizeOptionalAfterSalesText(value, maxLength, lengthMessage);
        if (normalized == null) throw new BusinessException(emptyMessage);
        return normalized;
    }

    private String normalizeOptionalAfterSalesText(String value, int maxLength, String lengthMessage) {
        if (StringUtils.isBlank(value)) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new BusinessException(lengthMessage);
        return normalized;
    }

    public void downloadImportTemplate(HttpServletResponse response) {
        excelUtil.writeTemplateToResponse(response,
                "客户导入模板",
                CUSTOMER_IMPORT_HEADERS,
                List.of(
                        List.of("示例客户", "直客（甲方）", "张三", "13900030001", "示例项目", "浙江杭州", "李经理"),
                        List.of("示例总包", "总包方", "王工", "13900030002", "", "", "")),
                List.of(
                        "仅支持 .xlsx 文件导入。",
                        "每行新增一个客户；客户名称和客户类型为必填项。",
                        "客户类型支持：直客（甲方）、总包方、分包方，也可填写 1、2、3。",
                        "联系人、联系电话、项目名称、施工区域、项目负责人均为可选项。",
                        "同一客户名称不能重复；系统不会覆盖已有客户。"),
                "客户导入模板.xlsx");
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importCustomers(MultipartFile file) {
        excelUtil.validateXlsxImportFile(file, MAX_IMPORT_FILE_SIZE_BYTES);
        ImportResultVO result = new ImportResultVO();
        Set<String> importedNames = new HashSet<>();
        try (var inputStream = file.getInputStream(); var workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            excelUtil.validateImportHeader(sheet.getRow(0), CUSTOMER_IMPORT_HEADERS);
            excelUtil.validateImportDataRows(sheet, CUSTOMER_IMPORT_COLUMN_COUNT, MAX_CUSTOMER_IMPORT_ROWS);
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (excelUtil.isEmptyRow(row, CUSTOMER_IMPORT_COLUMN_COUNT)) {
                    continue;
                }
                result.setTotalCount(result.getTotalCount() + 1);
                try {
                    CustomerAddRequest request = buildImportRequest(row);
                    String nameKey = request.getCustomerName().trim();
                    if (!importedNames.add(nameKey)) {
                        throw new BusinessException("客户名称在导入文件中重复：" + nameKey);
                    }
                    addCustomer(request);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception exception) {
                    result.setFailCount(result.getFailCount() + 1);
                    if (result.getFailMessages().size() < 20) {
                        result.getFailMessages().add("第 " + (index + 1) + " 行：" + importErrorMessage(exception));
                    }
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("读取客户导入文件失败");
        } catch (Exception exception) {
            throw new BusinessException("客户导入文件格式不正确，请使用系统下载的 .xlsx 模板");
        }
        return result;
    }

    private CustomerAddRequest buildImportRequest(Row row) {
        CustomerAddRequest request = new CustomerAddRequest();
        request.setCustomerName(requiredImportText(excelUtil.readString(row.getCell(0)), "客户名称不能为空"));
        request.setCustomerType(parseCustomerType(excelUtil.readString(row.getCell(1))));

        String contactName = excelUtil.readString(row.getCell(2));
        String contactPhone = excelUtil.readString(row.getCell(3));
        if (!contactName.isBlank() || !contactPhone.isBlank()) {
            CustomerContact contact = new CustomerContact();
            contact.setContactName(contactName.trim());
            contact.setContactPhone(contactPhone.trim());
            request.setContacts(List.of(contact));
        }

        String projectName = excelUtil.readString(row.getCell(4));
        String constructionArea = excelUtil.readString(row.getCell(5));
        String projectOwner = excelUtil.readString(row.getCell(6));
        if (!projectName.isBlank() || !constructionArea.isBlank() || !projectOwner.isBlank()) {
            if (projectName.isBlank()) {
                throw new BusinessException("填写施工区域或项目负责人时，项目名称不能为空");
            }
            CustomerProject project = new CustomerProject();
            project.setProjectName(projectName.trim());
            project.setConstructionArea(constructionArea.trim());
            project.setProjectOwner(projectOwner.trim());
            request.setProjects(List.of(project));
        }
        return request;
    }

    private Integer parseCustomerType(String value) {
        String type = requiredImportText(value, "客户类型不能为空");
        return switch (type.trim()) {
            case "1", "直客", "直客（甲方）" -> 1;
            case "2", "总包", "总包方" -> 2;
            case "3", "分包", "分包方" -> 3;
            default -> throw new BusinessException("客户类型仅支持：直客（甲方）、总包方、分包方");
        };
    }

    private String requiredImportText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String importErrorMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? "数据不合法" : exception.getMessage();
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
                project.setProjectOwner(StringUtils.isNotBlank(projectDto.getProjectOwner())
                        ? projectDto.getProjectOwner().trim()
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
