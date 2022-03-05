package az.kapitalbank.marketplace.client.dvs.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DvsClientException extends RuntimeException {

    private static final String MESSAGE = "Dvs client exception.method_key - %s, Response - %s";

    public DvsClientException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }
}
