package az.kapitalbank.marketplace.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
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
    CustomerInfo customerInfo;
    @NotNull
    Integer loanTerm;
    @NotNull
    BigDecimal totalAmount;
    List<OrderProductDeliveryInfo> deliveryInfo;
    List<@Valid OrderProductItem> products;
}
