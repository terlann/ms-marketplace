package az.kapitalbank.marketplace.client.dvs.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DvsCreateOrderResponse {
    Integer code;
    Boolean success;
    String orderId;
}
