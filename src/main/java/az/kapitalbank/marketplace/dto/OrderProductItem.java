package az.kapitalbank.marketplace.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("product_amount")
    Integer productAmount;
    @NotEmpty
    @JsonProperty("product_id")
    String productId;
    @NotEmpty
    @JsonProperty("product_name")
    String productName;
    @NotEmpty
    @JsonProperty("order_no")
    String orderNo;
    @NotEmpty
    @JsonProperty("item_type")
    String itemType;
    @NotEmpty
    @JsonProperty("partner_cms_id")
    String partnerCmsId;
}
