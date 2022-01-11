package az.kapitalbank.marketplace.exception;

import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.exception.models.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.models.OrderIsInactiveException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class ScoringOrderExceptionHandling extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OrderAlreadyScoringException.class)
    public ResponseEntity<WrapperResponseDto<?>> orderAlreadyScoring(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.ORDER_ALREADY_SCORING.getCode();
        var message = ex.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

    @ExceptionHandler(OrderIsInactiveException.class)
    public ResponseEntity<WrapperResponseDto<?>> orderIsDeactive(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.ORDER_INACTIVE.getCode();
        var message = ex.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

}
