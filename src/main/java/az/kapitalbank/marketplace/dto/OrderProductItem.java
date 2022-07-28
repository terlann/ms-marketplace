package az.kapitalbank.marketplace.dto;

import java.math.BigDecimal;
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
public class OrderProductItem {
    @NotNull
    BigDecimal productAmount;
    @NotBlank
    String productId;
    @NotBlank
    String productName;
    @NotBlank
    String orderNo;
    @NotBlank
    String itemType;
    @NotBlank
    String partnerCmsId;
    Integer categoryId;
    String brand;
}
