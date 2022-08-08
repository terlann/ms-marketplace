package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessStatus {
    public static final String FRAUD_PREFIX = "FRAUD_";
    public static final String BUSINESS_ERROR_PREFIX = "BUSINESS_ERROR_";
    public static final String BUSINESS_ERROR_EMPTY = "BUSINESS_ERROR_EMPTY";
    public static final String TELESALES_REJECT_CODE_PREFIX = "TELESALES_REJECT_CODE";

    public static final String SCORING_SCORED_AMOUNT_ZERO = "SCORING_SCORED_AMOUNT_ZERO";
    public static final String SCORING_NO_ENOUGH_AMOUNT = "SCORING_NO_ENOUGH_AMOUNT";
    public static final String DVS_GET_URL = "DVS_GET_URL";
    public static final String GET_DVS_URL_FAILED = "DVS_GET_URL_FAILED";
    public static final String GET_PROCESS_VARIABLE = "GET_PROCESS_VARIABLE";
    public static final String GET_PROCESS_VARIABLE_FAILED = "GET_PROCESS_VARIABLE_FAILED";
    public static final String FRAUD_CHECK = "FRAUD_CHECK";
    public static final String OTP_OPERATION = "FRAUD_CHECK";

    public static final String OPTIMUS_FAIL_COMPLETE_SCORING = "OPTIMUS_FAIL_COMPLETE_SCORING";
    public static final String OPTIMUS_FAIL_START_SCORING = "OPTIMUS_FAIL_START_SCORING";
    public static final String OPTIMUS_FAIL_CREATE_SCORING = "OPTIMUS_FAIL_CREATE_SCORING";
    public static final String OPTIMUS_FAIL_GET_PROCESS = "OPTIMUS_FAIL_GET_PROCESS";
    public static final String OPTIMUS_INCIDENT_HAPPENED = "OPTIMUS_INCIDENT_HAPPENED";
    public static final String OPTIMUS_NO_ENOUGH_AMOUNT = "OPTIMUS_NO_ENOUGH_AMOUNT";
    public static final String OPTIMUS_SCORED_AMOUNT_ZERO = "OPTIMUS_SCORED_AMOUNT_ZERO";
    public static final String DVS_FAIL_URL = "OPTIMUS_SCORED_AMOUNT_ZERO";
    public static final String DVS_NO_ACTION = "OPTIMUS_SCORED_AMOUNT_ZERO";
    public static final String DVS_REJECT = "DVS_REJECT";

}
