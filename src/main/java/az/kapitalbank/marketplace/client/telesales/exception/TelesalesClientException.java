package az.kapitalbank.marketplace.client.telesales.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TelesalesClientException extends RuntimeException {

    static final String MESSAGE = "Telesales client exception.method_key - %s, Response - %s";

    public TelesalesClientException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }
}
