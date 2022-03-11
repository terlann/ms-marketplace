package az.kapitalbank.marketplace.dto;

import java.math.BigDecimal;
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
public class DeliveryProductDto {
    @NotEmpty
    String orderNo;
    @NotEmpty
    String productId;
    @NotEmpty
    String itemType;
    @NotNull
    BigDecimal orderAmount;
}
