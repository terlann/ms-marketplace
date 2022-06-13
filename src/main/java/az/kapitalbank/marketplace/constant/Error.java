package az.kapitalbank.marketplace.constant;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@ToString
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum Error {

    INVALID_REQUEST("E100", "Request is not valid", BAD_REQUEST),
    ORDER_NOT_FOUND("E101", "Order not found", NOT_FOUND),
    NO_MATCH_LOAN_AMOUNT_BY_ORDERS("E102", "Loan amount is not equal total order amount",
            BAD_REQUEST),
    LOAN_TERM_NOT_FOUND("E103", "No such loan term", NOT_FOUND),
    PURCHASE_AMOUNT_LIMIT("E104",
            "Purchase amount must be between 50 and 15000 in first transaction", BAD_REQUEST),
    NO_ENOUGH_BALANCE("E105", "There is no enough amount in balance", BAD_REQUEST),
    CUSTOMER_NOT_COMPLETED_PROCESS("E106", "Customer has not yet completed the process",
            BAD_REQUEST),
    CUSTOMER_NOT_FOUND("E107", "Customer not found", NOT_FOUND),
    PERSON_NOT_FOUND("E108", "Person not found in IAMAS", NOT_FOUND),
    UMICO_USER_NOT_FOUND("E109", "Umico user not found", BAD_REQUEST),
    OPERATION_NOT_FOUND("E110", "Operation not found", NOT_FOUND),
    OPERATION_ALREADY_SCORED("E111", "Operation had already scored", BAD_REQUEST),
    UNIQUE_PHONE_NUMBER("E112", "Additional numbers must be different", BAD_REQUEST),
    ORDER_NOT_LINKED_TO_CUSTOMER("E113", "Order is not linked to customer", BAD_REQUEST),
    NO_PERMISSION("E114", "No permission refund/purchase", BAD_REQUEST),
    SERVICE_UNAVAILABLE("E115", "Service temporarily unavailable", BAD_REQUEST),
    OTP_PHONE_BLOCKED("E116", "Phone was blocked", BAD_REQUEST),
    OTP_SEND_LIMIT_EXCEEDED("E117", "Send otp limit exceed", BAD_REQUEST),
    OTP_NOT_FOUND("E118", "Otp not found", BAD_REQUEST),
    OTP_SERVICE_UNAVAILABLE("E119", "Otp service unavailable", BAD_REQUEST),
    OTP_ATTEMPT_LIMIT_ONE("E120", "Invalid otp! remaining attempt: 1", BAD_REQUEST),
    OTP_ATTEMPT_LIMIT_TWO("E121", "Invalid otp! remaining attempt: 2", BAD_REQUEST),
    SUBSCRIPTION_NOT_FOUND("E122", "Mobile number not found", BAD_REQUEST),
    INVALID_OTP_AND_PHONE_BLOCKED("E123", "Invalid otp. mobile number was blocked", BAD_REQUEST),
    NO_MATCH_ORDER_AMOUNT_BY_PRODUCTS("E124", "Order amount is not equal total products amount",
            BAD_REQUEST),
    PRODUCT_NOT_LINKED_TO_ORDER("E125", "Product is not linked to order", BAD_REQUEST),
    REFUND_FAILED("E126", "Refund operation couldn't finished", BAD_REQUEST),
    COMPLETE_PRE_PURCHASE_FAILED("E127", "Purchase operation couldn't finished", BAD_REQUEST),
    CUSTOMER_ID_SKIPPED("E128", "Customer id must not be null", BAD_REQUEST),
    NO_DELIVERY_PRODUCTS("E129", "No delivery products", BAD_REQUEST);

    String code;
    String message;
    HttpStatus status;
}
