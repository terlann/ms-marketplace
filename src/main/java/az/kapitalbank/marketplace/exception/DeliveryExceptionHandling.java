package az.kapitalbank.marketplace.exception;

import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.exception.models.LastAmountIncorrectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class DeliveryExceptionHandling extends ResponseEntityExceptionHandler {


    @ExceptionHandler(LastAmountIncorrectException.class)
    public ResponseEntity<WrapperResponseDto<?>> lastAmountIsInvalid(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.PRODUCT_AMOUNT_INCORRECT.getCode();
        var message = ex.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }


}
