package az.kapitalbank.marketplace.dto.request;

import java.util.List;
import java.util.UUID;

import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseRequestDto {
    String umicoUserId;
    UUID trackId;
    UUID customerId;
    List<DeliveryProductDto> deliveryOrders;
}
