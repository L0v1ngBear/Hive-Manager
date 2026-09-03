package my.hive.domain.customer.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.customer.model.entity.Customer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * Serializes create/rename attempts for a tenant's customer-name index.
     * InnoDB locks the matching row or its index gap until the outer
     * transaction commits, closing the check-then-insert race.
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id, tenant_code, customer_name, customer_type, construction_area,
                   customer_address, opening_date, source_type, import_time, import_user_id, import_user_name,
                   create_time, update_time
            FROM customer
            WHERE tenant_code = #{tenantCode}
              AND customer_name = #{customerName}
            LIMIT 1
            FOR UPDATE
            """)
    Customer selectByTenantCodeAndNameForUpdate(@Param("tenantCode") String tenantCode,
                                                @Param("customerName") String customerName);
}
