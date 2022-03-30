package az.kapitalbank.marketplace.client.umico.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UmicoClientException extends RuntimeException {

    static final String MESSAGE = "Umico client exception.method_key - %s, Response - %s";

    public UmicoClientException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }

    public UmicoClientException(String description) {
        super(description);
    }
}
