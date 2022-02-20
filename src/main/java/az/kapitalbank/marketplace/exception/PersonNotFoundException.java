package az.kapitalbank.marketplace.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PersonNotFoundException extends RuntimeException {
    private static final String PERSON_NOT_FOUND = "Person not found in IAMAS. %s";

    public PersonNotFoundException(String message) {
        super(String.format(PERSON_NOT_FOUND, message));
    }
}
