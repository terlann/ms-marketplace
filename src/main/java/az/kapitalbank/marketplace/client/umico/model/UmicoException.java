package az.kapitalbank.marketplace.client.umico.model;

import java.util.Collection;
import java.util.Map;

import feign.error.FeignExceptionConstructor;
import feign.error.ResponseBody;
import feign.error.ResponseHeaders;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UmicoException extends RuntimeException {

    UmicoError umicoError;
    Map<String, Collection<String>> headers;

    @FeignExceptionConstructor
    public UmicoException(@ResponseBody UmicoError umicoError,
                          @ResponseHeaders Map<String, Collection<String>> headers) {
        this.umicoError = umicoError;
        this.headers = headers;
    }
}
