package az.kapitalbank.marketplace.constant;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConstant {
    public static final String CUSTOMER_ID_LOG = "customerId - ";
    public static final String CUSTOMER_NOT_FOUND_LOG = "Customer not found : ";
    public static final String TELESALES_ORDER_ID_LOG = "telesalesOrderId - ";
    public static final String ORDER_NO_LOG = "orderNo - ";
    public static final String ORDER_NO_REQUEST_LOG = "orderNo - {}, request - {}";
    public static final String ORDER_NO_RESPONSE_LOG = "orderNo - {}, response - {}";
    public static final String ORDER_NO_EXCEPTION_LOG = "orderNo - {}, exception - {}";
    public static final String SECURITY_POLICY = "script-src 'self'; form-action 'self'";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final List<String> UMICO_REJECT_LIST_BY_FRAUD =
            List.of("BLACKLIST", "OTHER_UMICO_USER_ID_APPROVED_WITH_CURRENT_PIN");
}
