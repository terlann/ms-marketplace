package az.kapitalbank.marketplace.client.atlas.model.request;

import static az.kapitalbank.marketplace.constant.AtlasConstant.AZN;
import static az.kapitalbank.marketplace.constant.AtlasConstant.TERMINAL_NAME;

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
public class PrePurchaseRequest {
    BigDecimal amount;
    String pan;
    String rrn;
    String uid;
    String description;
    @Builder.Default
    Integer currency = AZN;
    @Builder.Default
    String terminalName = TERMINAL_NAME;
}
