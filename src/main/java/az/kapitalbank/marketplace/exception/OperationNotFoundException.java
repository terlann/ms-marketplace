package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OperationNotFoundException extends RuntimeException {

    static final String MESSAGE = "Operation not found. %s";

    public OperationNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
