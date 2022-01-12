package az.kapitalbank.marketplace.exception;

import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.exception.models.CreateTelesalesOrderException;
import az.kapitalbank.marketplace.exception.models.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.models.OrderAlreadyDeactivatedException;
import az.kapitalbank.marketplace.exception.models.OrderHaveNotBeenScoringException;
import az.kapitalbank.marketplace.exception.models.OrderNotFindException;
import az.kapitalbank.marketplace.exception.models.PhoneNumberInvalidException;
import az.kapitalbank.marketplace.exception.models.PinCodeInCorrectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class OrderExceptionHandling extends ResponseEntityExceptionHandler {


    @ExceptionHandler(PhoneNumberInvalidException.class)
    public ResponseEntity<WrapperResponseDto<?>> phoneNumberInvalid(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.PHONE_NUMBER_INVALID.getCode();
        var message = ErrorCodes.PHONE_NUMBER_INVALID.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

    @ExceptionHandler(PinCodeInCorrectException.class)
    public ResponseEntity<WrapperResponseDto<?>> pinCodeIsInvalid(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.PIN_CODE_INCORRECT.getCode();
        var message = ErrorCodes.PIN_CODE_INCORRECT.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

    @ExceptionHandler(CreateTelesalesOrderException.class)
    public ResponseEntity<WrapperResponseDto<?>> createTelesalesOrder(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.CREATE_TELESALES_ORDER.getCode();
        var message = ErrorCodes.CREATE_TELESALES_ORDER.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(wrapperResponseDto);
    }

    @ExceptionHandler(OrderNotFindException.class)
    public ResponseEntity<WrapperResponseDto<?>> orderIdNotFound(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.ORDER_NOT_FOUND.getCode();
        var message = ex.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(wrapperResponseDto);
    }

    @ExceptionHandler(OrderAlreadyDeactivatedException.class)
    public ResponseEntity<WrapperResponseDto<?>> orderAlreadyDeactivated(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.ORDER_ALREADY_DEACTIVATED.getCode();
        var message = ex.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

    @ExceptionHandler(OrderHaveNotBeenScoringException.class)
    public ResponseEntity<WrapperResponseDto<?>> orderHaveNotBeenScoring(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.ORDER_ALREADY_DEACTIVATED.getCode();
        var message = ex.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }


    @ExceptionHandler(LoanAmountIncorrectException.class)
    public ResponseEntity<WrapperResponseDto<?>> loanAmountIncorrect(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.LOAN_AMOUNT_INCORRECT.getCode();
        var message = ex.getMessage();
        var  wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }


}
