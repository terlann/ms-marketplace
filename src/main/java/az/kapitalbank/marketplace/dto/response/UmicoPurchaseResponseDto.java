package az.kapitalbank.marketplace.dto.response;

import az.kapitalbank.marketplace.constants.OrderStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UmicoPurchaseResponseDto {
    String orderNo;
    OrderStatus status;
}
