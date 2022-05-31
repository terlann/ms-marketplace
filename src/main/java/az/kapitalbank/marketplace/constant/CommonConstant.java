package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConstant {
    public static final String CUSTOMER_ID_LOG = "customerId - ";
    public static final String TELESALES_ORDER_ID_LOG = "telesalesOrderId - ";
    public static final String ORDER_NO_LOG = "orderNo - ";
    public static final String ORDER_NO_REQUEST_LOG = "orderNo - {}, request - {}";
    public static final String ORDER_NO_RESPONSE_LOG = "orderNo - {}, response - {}";
    public static final String ORDER_NO_EXCEPTION_LOG = "orderNo - {}, exception - {}";
}
