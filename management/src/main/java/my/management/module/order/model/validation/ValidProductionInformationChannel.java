package my.management.module.order.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProductionInformationChannelValidator.class)
public @interface ValidProductionInformationChannel {

    String message() default "生产订单信息渠道不能为空";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
