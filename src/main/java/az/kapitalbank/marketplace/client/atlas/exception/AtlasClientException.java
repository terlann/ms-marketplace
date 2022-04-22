package az.kapitalbank.marketplace.client.atlas.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasClientException extends RuntimeException {
    private final String uuid;
    private final String code;
    private final String message;

    public AtlasClientException(String uuid, String code, String message) {
        super(message);
        this.uuid = uuid;
        this.code = code;
        this.message = message;
    }
}
