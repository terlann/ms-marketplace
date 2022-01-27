package az.kapitalbank.marketplace.exception.handler;

import az.kapitalbank.marketplace.constants.ErrorCode;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class ScoringOrderExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OrderAlreadyScoringException.class)
    public ResponseEntity<WrapperResponseDto<?>> orderAlreadyScoring(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCode.ORDER_ALREADY_SCORING.getCode();
        var message = ex.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

}
