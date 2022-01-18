package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum ErrorCodes implements ErrorCode {

    INTERNAL_SERVER_ERROR("E101", "The service is temporary is unavailable"),
    REQUEST_VALIDATE("E102", "Request is not valid (Field is null or not valid) : %s"),
    REQUEST_FIELD_TYPES("E103", "Fields is unexpected types"),
    PHONE_NUMBER_INVALID("E111", "Phone number is invalid"),
    PIN_CODE_INCORRECT("E112", "Pin code is incorrect"),
    ORDER_NOT_FOUND("E113", "This order not found"),
    ORDER_ALREADY_SCORING("E114", "This order has already been scored"),
    ORDER_ALREADY_DELETED("E115", "This order already deleted"),
    ORDER_INACTIVE("E116", "Order is inactive"),
    ORDER_ALREADY_DEACTIVATED("E117", "Order already have been deactivated"),
    CREATE_TELESALES_ORDER("E118", "Cannot create telesales order.Please try after a while"),
    PRODUCT_AMOUNT_INCORRECT("E119", "The price of product is incorrect"),
    LOAN_AMOUNT_INCORRECT("E120", "The loan amount is incorrect");

    String code;
    String message;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
