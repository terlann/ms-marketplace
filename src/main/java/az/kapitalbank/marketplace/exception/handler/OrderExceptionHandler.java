package az.kapitalbank.marketplace.exception.handler;

import az.kapitalbank.marketplace.constants.ErrorCode;
import az.kapitalbank.marketplace.dto.ErrorResponseDto;
import az.kapitalbank.marketplace.exception.CreateTelesalesOrderException;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.MarketplaceException;
import az.kapitalbank.marketplace.exception.NoEnoughBalanceException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.PhoneNumberInvalidException;
import az.kapitalbank.marketplace.exception.PinCodeInCorrectException;
import az.kapitalbank.marketplace.exception.PinNotFoundException;
import az.kapitalbank.marketplace.exception.TotalAmountLimitException;
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

    @ExceptionHandler(MarketplaceException.class)
    public ResponseEntity<ErrorResponseDto> marketplaceException(MarketplaceException ex) {
        log.error("Exception: {}", ex);
        var errorResponse = ErrorResponseDto.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(PhoneNumberInvalidException.class)
    public ResponseEntity<ErrorResponseDto> phoneNumberInvalid(Exception ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.INVALID_MOBILE_NUMBER);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }
    @ExceptionHandler(PinCodeInCorrectException.class)
    public ResponseEntity<ErrorResponseDto> pinCodeIsInvalid(Exception ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.INCORRECT_PIN);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(PinNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> pinNotFound(PinNotFoundException ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.PIN_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(CreateTelesalesOrderException.class)
    public ResponseEntity<ErrorResponseDto> createTelesalesOrder(Exception ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.CREATE_TELESALES_ORDER);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponseDto);

    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> orderIdNotFound(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.ORDER_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(LoanAmountIncorrectException.class)
    public ResponseEntity<ErrorResponseDto> loanAmountIncorrect(LoanAmountIncorrectException e) {
        log.error("Exception: {}", e);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.LOAN_AMOUNT_INCORRECT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(UnknownLoanTerm.class)
    public ResponseEntity<ErrorResponseDto> loanTermIncorrect(Exception ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.LOAN_TERM_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(TotalAmountLimitException.class)
    public ResponseEntity<ErrorResponseDto> exceedTotalAmountLimit(TotalAmountLimitException ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.PURCHASE_AMOUNT_LIMIT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(NoEnoughBalanceException.class)
    public ResponseEntity<ErrorResponseDto> noEnoughBalance(NoEnoughBalanceException ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.NO_ENOUGH_BALANCE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }
}
