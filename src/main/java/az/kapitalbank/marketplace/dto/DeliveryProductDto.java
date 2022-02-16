package az.kapitalbank.marketplace.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryProductDto {
    @NotEmpty
    String orderNo;
    @NotEmpty
    String productId;
    @NotEmpty
    String itemType;
    @NotNull
    BigDecimal orderLastAmount;
}
