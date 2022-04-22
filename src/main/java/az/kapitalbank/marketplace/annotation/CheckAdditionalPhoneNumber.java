package az.kapitalbank.marketplace.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = CheckAdditionalPhoneNumberValidator.class)
public @interface CheckAdditionalPhoneNumber {
    String message() default "Required different additionalPhoneNumbers";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
