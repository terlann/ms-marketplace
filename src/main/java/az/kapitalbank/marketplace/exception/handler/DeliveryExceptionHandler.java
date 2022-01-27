package az.kapitalbank.marketplace.exception.handler;

import az.kapitalbank.marketplace.constants.ErrorCode;
import az.kapitalbank.marketplace.dto.ErrorResponseDto;
import az.kapitalbank.marketplace.exception.LastAmountIncorrectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class DeliveryExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(LastAmountIncorrectException.class)
    public ResponseEntity<ErrorResponseDto> lastAmountIsInvalid(LastAmountIncorrectException ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.PRODUCT_AMOUNT_INCORRECT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }
}
