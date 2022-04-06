package az.kapitalbank.marketplace.client.umico.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UmicoClientException extends RuntimeException {

    private static final String MESSAGE = "Umico client exception.method_key - %s, response - %s";

    public UmicoClientException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }

    public UmicoClientException(String description) {
        super(description);
    }
}
