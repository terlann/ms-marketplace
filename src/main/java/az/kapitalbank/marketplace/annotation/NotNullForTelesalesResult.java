package az.kapitalbank.marketplace.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = NotNullForTelesalesResultValidator.class)
public @interface NotNullForTelesalesResult {
    String message() default "Request fields must not be null in scoring status approved";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
