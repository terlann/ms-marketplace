package az.kapitalbank.marketplace.dto.request;

import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import java.util.Set;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
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
    UUID customerId;
    @NotNull
    UUID trackId;
    @NotEmpty
    String orderNo;
    @NotEmpty
    Set<@Valid DeliveryProductDto> deliveryProducts;
}
