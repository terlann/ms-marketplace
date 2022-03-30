package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OptimusConstant {
    public static final String PROCESS_KEY = "process_credit_online";
    public static final String PROCESS_PRODUCT_TYPE = "CONSUMER_LOAN_MARKET_PLACE_ONLINE";
    public static final String CARD_PRODUCT_CODE = "BUMM";
    public static final String CENTRAL_BRANCH_CODE = "299";
    public static final String ADDITIONAL_NAME_1 = "A";
    public static final String ADDITIONAL_NAME_2 = "B";
    public static final String SALES_SOURCE = "umico_marketplace";
    public static final String NAME_ON_CARD = "empty";
    public static final String OPTIMUS_CLIENT_EXCEPTION =
            "businessKey - {}, OptimusClientException - {}";
}
