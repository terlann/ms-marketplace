package az.kapitalbank.marketplace.constant;

import java.util.Arrays;

public enum FraudResultStatus {
    FRAUD_BLACKLIST,
    FRAUD_PIN_AND_UMICO_USER_ID_SUSPICIOUS,
    FRAUD_PIN_SUSPICIOUS,
    FRAUD_UMICO_USER_ID_SUSPICIOUS,
    FRAUD_OTHER_UMICO_USER_ID_REJECTED_WITH_CURRENT_PIN,
    FRAUD_OTHER_PIN_REJECTED_WITH_CURRENT_UMICO_USER_ID,
    FRAUD_OTHER_UMICO_USER_ID_APPROVED_WITH_CURRENT_PIN;

    public static boolean exists(FraudResultStatus fraudResultStatus) {
        return Arrays.stream(values()).anyMatch(value -> value == fraudResultStatus);
    }
}
