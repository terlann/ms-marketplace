package az.kapitalbank.marketplace.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestConstants {
    PIN("1234567"),
    MOBILE_NUMBER("994551112233"),
    RRN("29703c3e-9e41-11ec-b909-0242ac120002"),
    TASK_ID("3a30a65a-9bec-11ec-b909-0242ac120002"),
    CARD_UID("d2a9d8bc-9beb-11ec-b909-0242ac120002"),
    TRACK_ID("3a30a65a-9bec-11ec-b909-0242ac120002"),
    CUSTOMER_ID("30d8c6d0-a0fd-11ec-b909-0242ac120002"),
    UMICO_USER_ID("30d8c6d0-3242-11ec-b909-0242ac120002"),
    BUSINESS_KEY("d279220e-a1e9-11ec-b909-0242ac120002"),
    TELESALES_ORDER_ID("eb813fa4-a142-11ec-b909-0242ac120002"),
    TRANSACTION_ID("eb813fa4-a142-11ec-b925-0242ac120002");

    final String value;

}
