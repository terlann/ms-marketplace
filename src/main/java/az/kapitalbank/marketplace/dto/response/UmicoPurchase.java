package az.kapitalbank.marketplace.dto.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UmicoPurchase {
    String orderNo;
    String status;
}
