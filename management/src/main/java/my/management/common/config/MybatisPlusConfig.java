package my.management.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import my.management.common.context.TenantPermissionContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class MybatisPlusConfig {

    private static final Set<String> IGNORE_TENANT_TABLES = Set.of(
            "tenant",
            "user",
            "sys_user_role",
            "sys_role",
            "sys_role_permission",
            "sys_permission"
    );

    /*
    分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                String tenantCode = TenantPermissionContext.getTenantCode();
                return new StringValue(tenantCode);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_code";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                String tenantCode = TenantPermissionContext.getTenantCode();
                if (tenantCode == null || tenantCode.isBlank()) {
                    return true;
                }
                return IGNORE_TENANT_TABLES.contains(tableName.toLowerCase());
            }
        }));

        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
