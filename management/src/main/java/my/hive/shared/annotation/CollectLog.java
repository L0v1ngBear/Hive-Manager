package my.hive.shared.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务日志采集注解。
 * 标在 Controller 或 Service 方法上后，公共 AOP 会自动采集租户、用户、请求、耗时、异常等排查信息。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectLog {

    /**
     * 业务模块，例如 inventory、order、attendance。
     */
    String module();

    /**
     * 操作动作，例如 create、update、print、export。
     */
    String action();

    /**
     * 业务类型，可用于区分 sales_order、cloth、label 等。
     */
    String bizType() default "";

    /**
     * 业务编号表达式，支持 SpEL，例如 #request.orderId、#p0、#a0.id。
     */
    String bizNo() default "";

    /**
     * Business number expression evaluated after a successful invocation.
     * Use {@code #result} when the created business number is returned by the method.
     */
    String resultBizNo() default "";

    /**
     * 人可读描述，方便日志平台检索。
     */
    String description() default "";

    /**
     * 是否记录入参。默认记录，但会自动脱敏并截断。
     */
    boolean recordArgs() default true;

    /**
     * 是否记录返回值。返回值可能较大，默认关闭。
     */
    boolean recordResult() default false;

    /**
     * 慢操作阈值。小于 0 表示使用全局配置。
     */
    long slowThresholdMs() default -1L;
}
