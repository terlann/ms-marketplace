package az.kapitalbank.marketplace.client.atlas.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AtlasClientException extends RuntimeException {
    String uuid;
    String code;
    String message;
}
