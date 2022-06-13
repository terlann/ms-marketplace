package az.kapitalbank.marketplace.exception.handler;

import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.dto.ErrorResponseDto;
import az.kapitalbank.marketplace.exception.CommonException;
import az.kapitalbank.marketplace.exception.DeliveryException;
import az.kapitalbank.marketplace.exception.OtpException;
import az.kapitalbank.marketplace.exception.PaybackException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String EXCEPTION = "Error Response - {} , Exception - {}";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        log.error("Request - {}, Exception: {}", request.toString(), ex);
        Map<String, String> warnings = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            warnings.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        var errorResponseDto = new ErrorResponseDto(Error.INVALID_REQUEST, warnings);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorResponseDto> commonException(
            CommonException ex) {
        log.error(EXCEPTION, ex.getError(), ex);
        var errorResponseDto = new ErrorResponseDto(ex.getError());
        return ResponseEntity.status(ex.getError().getStatus()).body(errorResponseDto);
    }

    @ExceptionHandler(PaybackException.class)
    public ResponseEntity<ErrorResponseDto> paybackException(PaybackException ex) {
        log.error(EXCEPTION, Error.REFUND_FAILED, ex);
        var errorResponseDto = new ErrorResponseDto(Error.REFUND_FAILED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(DeliveryException.class)
    public ResponseEntity<ErrorResponseDto> deliveryException(DeliveryException ex) {
        log.error(EXCEPTION, Error.COMPLETE_PRE_PURCHASE_FAILED, ex);
        var errorResponseDto = new ErrorResponseDto(Error.COMPLETE_PRE_PURCHASE_FAILED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponseDto> otpClientException(OtpException ex) {
        Error error;
        switch (ex.getMessage()) {
            case "phone blocked":
                error = Error.OTP_PHONE_BLOCKED;
                break;
            case "send otp limit exceeded":
                error = Error.OTP_SEND_LIMIT_EXCEEDED;
                break;
            case "otp not found":
                error = Error.OTP_NOT_FOUND;
                break;
            case "invalid otp. remaining attempt: 2":
                error = Error.OTP_ATTEMPT_LIMIT_TWO;
                break;
            case "invalid otp. remaining attempt: 1":
                error = Error.OTP_ATTEMPT_LIMIT_ONE;
                break;
            case "invalid otp. user blocked":
                error = Error.INVALID_OTP_AND_PHONE_BLOCKED;
                break;
            default:
                error = Error.OTP_SERVICE_UNAVAILABLE;
        }
        var errorResponseDto = new ErrorResponseDto(error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }
}
