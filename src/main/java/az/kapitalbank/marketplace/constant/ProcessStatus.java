package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessStatus {
    public static final String FRAUD_PREFIX = "FRAUD_";
    public static final String BUSINESS_ERROR_PREFIX = "BUSINESS_ERROR_";
    public static final String BUSINESS_ERROR_EMPTY = "BUSINESS_ERROR_EMPTY";
    public static final String TELESALES_REJECT_CODE_PREFIX = "TELESALES_REJECT_CODE_";
    public static final String OPTIMUS_FAIL_COMPLETE_SCORING = "OPTIMUS_FAIL_COMPLETE_SCORING";
    public static final String OPTIMUS_FAIL_START_SCORING = "OPTIMUS_FAIL_START_SCORING";
    public static final String OPTIMUS_FAIL_CREATE_SCORING = "OPTIMUS_FAIL_CREATE_SCORING";
    public static final String OPTIMUS_FAIL_GET_PROCESS = "OPTIMUS_FAIL_GET_PROCESS";
    public static final String OPTIMUS_INCIDENT_HAPPENED = "OPTIMUS_INCIDENT_HAPPENED";
    public static final String OPTIMUS_NO_ENOUGH_AMOUNT = "OPTIMUS_NO_ENOUGH_AMOUNT";
    public static final String OPTIMUS_SCORED_AMOUNT_ZERO = "OPTIMUS_SCORED_AMOUNT_ZERO";
    public static final String DVS_FAIL_URL = "DVS_FAIL_URL";
    public static final String DVS_NO_ACTION = "DVS_NO_ACTION";
    public static final String DVS_REJECT = "DVS_REJECT";

}
