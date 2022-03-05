package az.kapitalbank.marketplace.client.integration.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class IamasClientException extends RuntimeException {
    private static final String MESSAGE = "Iamas client exception. method_key - %s, Response - %s";

    public IamasClientException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }
}
