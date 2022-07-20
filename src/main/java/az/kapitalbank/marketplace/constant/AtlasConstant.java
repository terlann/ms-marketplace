package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AtlasConstant {
    public static final Integer AZN = 944;
    public static final String TERMINAL_NAME = "UMICOLOAN";
    public static final String PRE_PURCHASE_DESCRIPTION =
            "umico pre-purchase operation, orderNo : ";
    public static final String COMPLETE_PRE_PURCHASE_DESCRIPTION =
            "umico complete-pre-purchase operation, orderNo : ";
    public static final String COMPLETE_PRE_PURCHASE_FOR_REFUND_DESCRIPTION =
            "umico complete-pre-purchase operation for refund, orderNo : ";
}
