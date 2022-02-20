package az.kapitalbank.marketplace.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UmicoUserNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Umico user not found. umico_user_id - %S";

    public UmicoUserNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
