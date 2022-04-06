package az.kapitalbank.marketplace.dto.request;

import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
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
public class CreateOrderRequestDto {
    @NotNull
    CustomerInfo customerInfo;
    @NotNull
    @Min(1)
    Integer loanTerm;
    @NotNull
    BigDecimal totalAmount;
    @NotEmpty
    List<@Valid OrderProductDeliveryInfo> deliveryInfo;
    @NotEmpty
    List<@Valid OrderProductItem> products;
}
