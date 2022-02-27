package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UniqueAdditionalNumberException extends RuntimeException {

    static String MESSAGE = "Additional numbers aren't different.additionalPhoneNumber1=%s , additionalPhoneNumber2=%s";

    public UniqueAdditionalNumberException(String additionalPhoneNumber1, String additionalPhoneNumber2) {
        super(String.format(MESSAGE, additionalPhoneNumber1, additionalPhoneNumber2));
    }
}
