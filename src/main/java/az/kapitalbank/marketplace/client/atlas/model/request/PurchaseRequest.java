package az.kapitalbank.marketplace.client.atlas.model.request;

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
    private Integer currency;
    private String pan;
    private String rrn;
    private String uid;
    private String description;
    private String terminalName;
}
