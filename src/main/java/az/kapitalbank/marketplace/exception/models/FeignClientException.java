package az.kapitalbank.marketplace.exception.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FeignClientException extends RuntimeException {

    static final String MESSAGE = "feign client exception.method_key - %s, Response - %s";

    public FeignClientException(String methodKey, String response) {
        super(String.format(MESSAGE, methodKey, response));
    }
}
