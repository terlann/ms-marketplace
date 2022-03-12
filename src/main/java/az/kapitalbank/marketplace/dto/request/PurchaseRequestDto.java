package az.kapitalbank.marketplace.dto.request;

import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
