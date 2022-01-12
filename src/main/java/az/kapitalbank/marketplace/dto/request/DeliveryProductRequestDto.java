package az.kapitalbank.marketplace.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryProductRequestDto {
    @JsonProperty("marketplace_track_id")
    String marketplaceTrackId;
    @JsonProperty("ete_order_id")
    String eteOrderId;
    @NotNull
    @JsonProperty("delivery_products")
    List<@Valid DeliveryProductDto> products;
}
