package az.kapitalbank.marketplace.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DeliveryProductDto {
    @NotEmpty
    String orderNo;
    @NotEmpty
    String productId;
    @NotEmpty
    String itemType;
    @NotNull
    BigDecimal lastAmount;
}
