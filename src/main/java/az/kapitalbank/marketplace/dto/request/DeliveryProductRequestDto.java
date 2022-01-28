package az.kapitalbank.marketplace.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryProductRequestDto {
    String marketplaceTrackId;
    String eteOrderId;
    @NotNull
    List<@Valid DeliveryProductDto> products;
}
