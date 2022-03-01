package az.kapitalbank.marketplace.client.optimus.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OptimusClientException extends RuntimeException {

    private static final String MESSAGE = "Optimus client exception.method_key - %s, Response - %s";

    public OptimusClientException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }
}
