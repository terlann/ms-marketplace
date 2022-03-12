package az.kapitalbank.marketplace.client.atlas.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasClientException extends RuntimeException {
    private final UUID uuid;
    private final String code;
    private final String message;

    public AtlasClientException(java.util.UUID uuid, String code, String message) {
        super(message);
        this.uuid = uuid;
        this.code = code;
        this.message = message;
    }
}
