package my.management.common.utils;

import jakarta.annotation.Resource;
import my.hive.common.context.TenantPermissionContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
/**
 * CodeGeneratorUtil 属于管理端后端通用能力层，提供可复用的工具方法。
 */
@Component
public class CodeGeneratorUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DEFAULT_TENANT = "0000"; // 更规范的默认租户
    private static final long DEFAULT_EXPIRE_HOURS = 25;

    public String generateCode(String prefix, int length) {
        try {
            // 1. 获取租户（空值安全处理）
            String tenantCode = TenantPermissionContext.getTenantCode();

            // 2. 日期
            String dateStr = LocalDateTime.now().format(DATE_FORMATTER);

            // 3. Redis Key
            String redisKey = String.format("sys:seq:%s:%s:%s", tenantCode, prefix, dateStr);

            // 4. 原子自增 + 过期时间（原子操作，无并发问题）
            Long increment = stringRedisTemplate.opsForValue().increment(redisKey, 1);
            stringRedisTemplate.expire(redisKey, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);

            // 5. 空值兜底
            long seq = (increment == null) ? 1L : increment;

            // 6. 格式化补 0
            String seqStr = String.format("%0" + length + "d", seq);

            // 7. 最终编号
            return prefix + dateStr + seqStr;

        } catch (Exception e) {
            // Redis 异常降级：使用时间戳后6位 + 随机数，保证不重复、不报错
            String random = String.format("%06d", (int) (Math.random() * 1000000));
            return prefix + LocalDateTime.now().format(DATE_FORMATTER) + random;
        }
    }

    public String generateEmployeeNo() {
        return generateCode("EMP", 4);
    }

    public String generateRoleCode() {
        return generateCode("ROLE", 4);
    }

    /**
     * 生成销售订单编号，和小程序端保持同一套前缀规则，方便后续跨端排查。
     */
    public String generateSalesOrderCode() {
        return generateCode("SO", 4);
    }

    /**
     * 生成生产订单编号，统一由后端生成，避免管理端和小程序端格式漂移。
     */
    public String generateProductionOrderCode() {
        return generateCode("PO", 4);
    }
}
