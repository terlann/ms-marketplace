package az.kapitalbank.marketplace.exception;

import az.kapitalbank.marketplace.constants.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MarketplaceException extends RuntimeException {

    final String message;
    final ErrorCode errorCode;
    final HttpStatus httpStatus;

    public MarketplaceException(String message, ErrorCode errorCode, HttpStatus httpStatus) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
