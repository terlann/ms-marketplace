package az.kapitalbank.marketplace.client.atlas.model.request;

import static az.kapitalbank.marketplace.constant.AtlasConstant.AZN;
import static az.kapitalbank.marketplace.constant.AtlasConstant.TERMINAL_NAME;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {
    private BigDecimal amount;
    private String pan;
    private String rrn;
    private String uid;
    private String description;
    @Builder.Default
    private Integer currency = AZN;
    @Builder.Default
    private String terminalName = TERMINAL_NAME;
}
