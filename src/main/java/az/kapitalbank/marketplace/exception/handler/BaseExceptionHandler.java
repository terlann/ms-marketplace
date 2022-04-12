package az.kapitalbank.marketplace.exception.handler;

import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.integration.exception.IamasClientException;
import az.kapitalbank.marketplace.client.otp.exception.OtpClientException;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.dto.ErrorResponseDto;
import az.kapitalbank.marketplace.exception.CustomerNotCompletedProcessException;
import az.kapitalbank.marketplace.exception.CustomerNotFoundException;
import az.kapitalbank.marketplace.exception.NoEnoughBalanceException;
import az.kapitalbank.marketplace.exception.NoMatchLoanAmountByOrderException;
import az.kapitalbank.marketplace.exception.NoMatchOrderAmountByProductException;
import az.kapitalbank.marketplace.exception.NoPermissionForTransaction;
import az.kapitalbank.marketplace.exception.OperationAlreadyScoredException;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.OrderNotLinkedToCustomer;
import az.kapitalbank.marketplace.exception.PersonNotFoundException;
import az.kapitalbank.marketplace.exception.ProductNotLinkedToOrder;
import az.kapitalbank.marketplace.exception.SubscriptionNotFoundException;
import az.kapitalbank.marketplace.exception.TotalAmountLimitException;
import az.kapitalbank.marketplace.exception.UmicoUserNotFoundException;
import az.kapitalbank.marketplace.exception.UniqueAdditionalNumberException;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
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

    private static final String EXCEPTION = "Exception: {}";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        log.error("Request - {}, Exception: ", request.toString(), ex);
        Map<String, String> warnings = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            warnings.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        var errorResponseDto = new ErrorResponseDto(Error.BAD_REQUEST, warnings);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(AtlasClientException.class)
    public ResponseEntity<ErrorResponseDto> atlasClientException(AtlasClientException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.SERVICE_UNAVAILABLE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler({PersonNotFoundException.class, IamasClientException.class})
    public ResponseEntity<ErrorResponseDto> personNotFound(Exception ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.PERSON_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> orderIdNotFound(OrderNotFoundException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.ORDER_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(NoMatchLoanAmountByOrderException.class)
    public ResponseEntity<ErrorResponseDto> ordersTotalAmountIncorrect(
            NoMatchLoanAmountByOrderException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.NO_MATCH_LOAN_AMOUNT_BY_ORDERS);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(NoMatchOrderAmountByProductException.class)
    public ResponseEntity<ErrorResponseDto> productsTotalAmountIncorrect(
            NoMatchOrderAmountByProductException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.NO_MATCH_ORDER_AMOUNT_BY_PRODUCTS);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(UnknownLoanTerm.class)
    public ResponseEntity<ErrorResponseDto> loanTermIncorrect(UnknownLoanTerm ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.LOAN_TERM_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(TotalAmountLimitException.class)
    public ResponseEntity<ErrorResponseDto> exceedTotalAmountLimit(TotalAmountLimitException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.PURCHASE_AMOUNT_LIMIT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(NoEnoughBalanceException.class)
    public ResponseEntity<ErrorResponseDto> noEnoughBalance(NoEnoughBalanceException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.NO_ENOUGH_BALANCE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(OperationAlreadyScoredException.class)
    public ResponseEntity<ErrorResponseDto> alreadyScored(OperationAlreadyScoredException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.OPERATION_ALREADY_SCORED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(OperationNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> alreadyScored(OperationNotFoundException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.OPERATION_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> customerNotFoundException(
            CustomerNotFoundException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.CUSTOMER_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(UmicoUserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> umicoUserNotFoundException(
            UmicoUserNotFoundException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.UMICO_USER_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(CustomerNotCompletedProcessException.class)
    public ResponseEntity<ErrorResponseDto> customerNotCompletedProcessException(
            CustomerNotCompletedProcessException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.CUSTOMER_NOT_COMPLETED_PROCESS);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(UniqueAdditionalNumberException.class)
    public ResponseEntity<ErrorResponseDto> uniqueAdditionalNumberException(
            UniqueAdditionalNumberException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.UNIQUE_PHONE_NUMBER);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(OrderNotLinkedToCustomer.class)
    public ResponseEntity<ErrorResponseDto> orderNotLinkedToCustomerException(
            OrderNotLinkedToCustomer ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.ORDER_NOT_LINKED_TO_CUSTOMER);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(ProductNotLinkedToOrder.class)
    public ResponseEntity<ErrorResponseDto> productNotLinkedToOrderException(
            ProductNotLinkedToOrder ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.PRODUCT_NOT_LINKED_TO_ORDER);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(NoPermissionForTransaction.class)
    public ResponseEntity<ErrorResponseDto> orderMustNotCompleteException(
            NoPermissionForTransaction ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.NO_PERMISSION);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> subscriptionNotFoundException(
            SubscriptionNotFoundException ex) {
        log.error(EXCEPTION, ex);
        var errorResponseDto = new ErrorResponseDto(Error.SUBSCRIPTION_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    @ExceptionHandler(OtpClientException.class)
    public ResponseEntity<ErrorResponseDto> otpClientException(OtpClientException ex) {
        log.error(ex.toString());
        Error error;
        switch (ex.getDetail()) {
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
