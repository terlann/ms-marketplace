package az.kapitalbank.marketplace.exception.handler;

import az.kapitalbank.marketplace.constants.ErrorCode;
import az.kapitalbank.marketplace.dto.ErrorResponseDto;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.exception.CreateTelesalesOrderException;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.PhoneNumberInvalidException;
import az.kapitalbank.marketplace.exception.PinCodeInCorrectException;
import az.kapitalbank.marketplace.exception.PinNotFoundException;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class OrderExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(PhoneNumberInvalidException.class)
    public ResponseEntity<WrapperResponseDto<?>> phoneNumberInvalid(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCode.INVALID_MOBILE_NUMBER.getCode();
        var message = ErrorCode.INVALID_MOBILE_NUMBER.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

    @ExceptionHandler(PinCodeInCorrectException.class)
    public ResponseEntity<WrapperResponseDto<?>> pinCodeIsInvalid(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCode.INCORRECT_PIN.getCode();
        var message = ErrorCode.INCORRECT_PIN.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }

    @ExceptionHandler(PinNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> pinNotFound(PinNotFoundException ex) {
        log.error("Exception: {}", ex);
        var code = ErrorCode.PIN_NOT_FOUND.getCode();
        var message = String.format(ErrorCode.PIN_NOT_FOUND.getMessage(), ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(code, message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(CreateTelesalesOrderException.class)
    public ResponseEntity<WrapperResponseDto<?>> createTelesalesOrder(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCode.CREATE_TELESALES_ORDER.getCode();
        var message = ErrorCode.CREATE_TELESALES_ORDER.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(wrapperResponseDto);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<WrapperResponseDto<?>> orderIdNotFound(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCode.ORDER_NOT_FOUND.getCode();
        var message = ex.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(wrapperResponseDto);
    }

    @ExceptionHandler(LoanAmountIncorrectException.class)
    public ResponseEntity<ErrorResponseDto> loanAmountIncorrect(LoanAmountIncorrectException e) {
        log.error("Exception: {}", e);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.LOAN_AMOUNT_INCORRECT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(UnknownLoanTerm.class)
    public ResponseEntity<WrapperResponseDto<?>> loanAmountIncorrect(Exception ex) {
        log.error("Exception: {}", ex);
        var code = HttpStatus.NOT_FOUND.value();
        var message = ex.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(String.valueOf(code), message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(wrapperResponseDto);
    }

}
