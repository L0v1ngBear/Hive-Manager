package my.management.module.order.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import my.management.module.order.model.dto.ProductionOrderSaveRequest;
import my.management.module.order.model.enums.OrderCategoryEnum;
import org.springframework.util.StringUtils;

public class ProductionInformationChannelValidator
        implements ConstraintValidator<ValidProductionInformationChannel, ProductionOrderSaveRequest> {

    @Override
    public boolean isValid(ProductionOrderSaveRequest request, ConstraintValidatorContext context) {
        if (request == null
                || OrderCategoryEnum.DRAWING_BUDGET.getCode().equals(
                OrderCategoryEnum.normalize(request.getOrderCategory()))
                || StringUtils.hasText(request.getInformationChannel())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("informationChannel")
                .addConstraintViolation();
        return false;
    }
}
