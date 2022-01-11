package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTelesalesOrderException extends RuntimeException {

    static final String MESSAGE = "Telesales order cannot create. Request - [%s]";

    public CreateTelesalesOrderException(Object request) {
        super(String.format(MESSAGE, request.toString()));
    }
}
