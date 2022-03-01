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
    NO_MATCH_LOAN_AMOUNT("E102", "Loan amount is not equal total order amount"),
    LOAN_TERM_NOT_FOUND("E103", "No such loan term"),
    PURCHASE_AMOUNT_LIMIT("E104", "Purchase amount must be between 50 and 20000 in first transaction"),
    NO_ENOUGH_BALANCE("E105", "There is no enough amount in balance"),
    CUSTOMER_NOT_COMPLETED_PROCESS("E106", "Customer has not yet completed the process"),
    CUSTOMER_NOT_FOUND("E107", "Customer not found"),
    PERSON_NOT_FOUND("E108", "Person not found in IAMAS"),
    UMICO_USER_NOT_FOUND("E109", "Umico user not found"),
    OPERATION_NOT_FOUND("E110", "Operation not found"),
    OPERATION_ALREADY_SCORED("E111", "Operation had already scored"),
    UNIQUE_PHONE_NUMBER("E112", "Additional numbers must be different"),
    ORDER_NOT_LINKED_TO_CUSTOMER("E113", "Order is not linked to customer");

    String code;
    String message;
}
