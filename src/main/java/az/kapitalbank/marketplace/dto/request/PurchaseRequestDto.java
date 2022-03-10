package az.kapitalbank.marketplace.dto.request;

import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseRequestDto {
    @NotBlank
    String umicoUserId;
    @NotNull
    UUID trackId;
    @NotNull
    UUID customerId;
    List<DeliveryProductDto> deliveryOrders;
}
