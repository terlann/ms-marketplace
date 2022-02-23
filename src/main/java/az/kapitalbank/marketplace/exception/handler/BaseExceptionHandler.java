package az.kapitalbank.marketplace.exception.handler;

import javax.validation.UnexpectedTypeException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

import az.kapitalbank.marketplace.client.externalinteg.exception.ExternalIntegrationException;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.dto.ErrorResponseDto;
import az.kapitalbank.marketplace.exception.CustomerNotCompletedProcessException;
import az.kapitalbank.marketplace.exception.CustomerNotFoundException;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.NoEnoughBalanceException;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.PersonNotFoundException;
import az.kapitalbank.marketplace.exception.TotalAmountLimitException;
import az.kapitalbank.marketplace.exception.UmicoUserNotFoundException;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
import feign.FeignException;
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

    @ExceptionHandler({SQLIntegrityConstraintViolationException.class,
            SQLException.class,
            FeignException.class})
    public ResponseEntity<ErrorResponseDto> multipleException(Exception ex) {
        log.error("Exception: {}", ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseEntity<ErrorResponseDto> unexpectedTypeException(UnexpectedTypeException ex) {
        log.error("UnexpectedTypeException: {}", ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.REQUEST_FIELD_TYPES);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler({PersonNotFoundException.class, ExternalIntegrationException.class})
    public ResponseEntity<ErrorResponseDto> personNotFound(Exception ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.PERSON_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> orderIdNotFound(OrderNotFoundException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.ORDER_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(LoanAmountIncorrectException.class)
    public ResponseEntity<ErrorResponseDto> loanAmountIncorrect(LoanAmountIncorrectException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.LOAN_AMOUNT_INCORRECT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(UnknownLoanTerm.class)
    public ResponseEntity<ErrorResponseDto> loanTermIncorrect(UnknownLoanTerm ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.LOAN_TERM_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(TotalAmountLimitException.class)
    public ResponseEntity<ErrorResponseDto> exceedTotalAmountLimit(TotalAmountLimitException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.PURCHASE_AMOUNT_LIMIT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(NoEnoughBalanceException.class)
    public ResponseEntity<ErrorResponseDto> noEnoughBalance(NoEnoughBalanceException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.NO_ENOUGH_BALANCE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(OrderAlreadyScoringException.class)
    public ResponseEntity<ErrorResponseDto> orderAlreadyScoring(OrderAlreadyScoringException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.ORDER_ALREADY_SCORING);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> customerNotFoundException(CustomerNotFoundException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.CUSTOMER_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(UmicoUserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> umicoUserNotFoundException(UmicoUserNotFoundException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.UMICO_USER_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(CustomerNotCompletedProcessException.class)
    public ResponseEntity<ErrorResponseDto> customerNotCompletedProcessException(
            CustomerNotCompletedProcessException ex) {
        log.error(ex.getMessage());
        var errorResponseDto = new ErrorResponseDto(Error.CUSTOMER_NOT_COMPLETED_PROCESS);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        log.error("Request - [{}], Exception: ", request.toString(), ex);
        Map<String, String> warnings = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors())
            warnings.put(fieldError.getField(), fieldError.getDefaultMessage());

        var code = Error.BAD_REQUEST.getCode();
        var message = Error.BAD_REQUEST.getMessage();
        var errorResponseDto = new ErrorResponseDto(code, message, warnings);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);

    }
}
