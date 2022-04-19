package az.kapitalbank.marketplace.annotation;

import az.kapitalbank.marketplace.dto.request.CustomerInfo;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckAdditionalPhoneNumberValidator
        implements ConstraintValidator<CheckAdditionalPhoneNumber, CustomerInfo> {

    @Override
    public boolean isValid(CustomerInfo value, ConstraintValidatorContext context) {
        return !(value.getCustomerId() == null
                && (value.getAdditionalPhoneNumber1() == null
                || value.getAdditionalPhoneNumber2() == null
                || value.getAdditionalPhoneNumber1()
                .equals(value.getAdditionalPhoneNumber2())));
    }
}
