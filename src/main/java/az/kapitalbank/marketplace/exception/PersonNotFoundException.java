package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonNotFoundException extends RuntimeException {

    static String PERSON_NOT_FOUND = "Person not found in IAMAS. %s";

    public PersonNotFoundException(String message) {
        super(String.format(PERSON_NOT_FOUND, message));
    }
}
