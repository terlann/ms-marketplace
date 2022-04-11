package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum Error {

    BAD_REQUEST("E100", "Request is not valid (Field is null or not valid) : %s"),
    ORDER_NOT_FOUND("E101", "Order not found"),
    NO_MATCH_LOAN_AMOUNT_BY_ORDERS("E102", "Loan amount is not equal total order amount"),
    NO_MATCH_ORDER_AMOUNT_BY_PRODUCTS("E102", "Order amount is not equal total products amount"),
    LOAN_TERM_NOT_FOUND("E103", "No such loan term"),
    PURCHASE_AMOUNT_LIMIT("E104",
            "Purchase amount must be between 50 and 20000 in first transaction"),
    NO_ENOUGH_BALANCE("E105", "There is no enough amount in balance"),
    CUSTOMER_NOT_COMPLETED_PROCESS("E106", "Customer has not yet completed the process"),
    CUSTOMER_NOT_FOUND("E107", "Customer not found"),
    PERSON_NOT_FOUND("E108", "Person not found in IAMAS"),
    UMICO_USER_NOT_FOUND("E109", "Umico user not found"),
    OPERATION_NOT_FOUND("E110", "Operation not found"),
    OPERATION_ALREADY_SCORED("E111", "Operation had already scored"),
    UNIQUE_PHONE_NUMBER("E112", "Additional numbers must be different"),
    ORDER_NOT_LINKED_TO_CUSTOMER("E113", "Order is not linked to customer"),
    NO_PERMISSION("E114", "No permission reverse/purchase"),
    SERVICE_UNAVAILABLE("E115", "Service temporarily unavailable"),
    OTP_PHONE_BLOCKED("E116", "Phone was blocked"),
    OTP_SEND_LIMIT_EXCEEDED("E117", "Send otp limit exceed"),
    OTP_NOT_FOUND("E118", "Otp not found"),
    OTP_SERVICE_UNAVAILABLE("E119", "Otp service unavailable"),
    OTP_ATTEMPT_LIMIT_ONE("E120", "Invalid otp! remaining attempt: 1"),
    OTP_ATTEMPT_LIMIT_TWO("E121", "Invalid otp! remaining attempt: 2"),
    SUBSCRIPTION_NOT_FOUND("E122", "Mobile number not found"),
    INVALID_OTP_AND_PHONE_BLOCKED("E123", "Invalid otp. mobile number was blocked");

    String code;
    String message;
}
