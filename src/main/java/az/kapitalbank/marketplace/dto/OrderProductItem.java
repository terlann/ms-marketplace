package az.kapitalbank.marketplace.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

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
public class OrderProductItem {
    @NotNull
    BigDecimal productAmount;
    @NotEmpty
    String productId;
    @NotEmpty
    String productName;
    @NotEmpty
    String orderNo;
    @NotEmpty
    String itemType;
    @NotEmpty
    String partnerCmsId;
}
