package az.kapitalbank.marketplace.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("E101", "The service is temporary is unavailable"),
    BAD_REQUEST("E102", "Request is not valid (Field is null or not valid) : %s"),
    REQUEST_FIELD_TYPES("E103", "Fields is unexpected types"),
    INVALID_MOBILE_NUMBER("E111", "Phone number is invalid"),
    INCORRECT_PIN("E112", "Pin code is incorrect"),
    ORDER_NOT_FOUND("E113", "This order not found"),
    ORDER_ALREADY_SCORING("E114", "This order has already been scored"),
    ORDER_INACTIVE("E116", "Order is inactive"),
    ORDER_ALREADY_DEACTIVATED("E117", "Order already have been deactivated"),
    CREATE_TELESALES_ORDER("E118", "Cannot create telesales order.Please try after a while"),
    PRODUCT_AMOUNT_INCORRECT("E119", "The price of product is incorrect"),
    LOAN_AMOUNT_INCORRECT("E120", "The loan amount is incorrect"),
    PIN_NOT_FOUND("E121", "Pin not found : %s"),
    LOAN_TERM_NOT_FOUND("E122", "No such loan term"),
    PURCHASE_AMOUNT_LIMIT("E123", "Purchase amount must be between 50 and 20000 in first transaction"),
    NO_ENOUGH_BALANCE("E124", "There is no enough amount in balance");

    String code;
    String message;
}
