package az.kapitalbank.marketplace.dto.response;

import az.kapitalbank.marketplace.constant.OrderStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseResponseDto {
    String orderNo;
    OrderStatus status;
}
