package az.kapitalbank.marketplace.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonNaming {

    public Class<? extends PropertyNameStrategy> value() default PropertyNameStrategy.class;
}
