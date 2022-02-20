package az.kapitalbank.marketplace.client.externalinteg.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ExternalIntegrationException extends RuntimeException {
    private static final String MESSAGE = "ExternalIntegration client exception. method_key - %s, Response - %s";

    public ExternalIntegrationException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }
}
