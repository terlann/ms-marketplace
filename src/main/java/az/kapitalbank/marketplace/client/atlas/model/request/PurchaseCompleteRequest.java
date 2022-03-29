package az.kapitalbank.marketplace.client.atlas.model.request;

import static az.kapitalbank.marketplace.constant.AtlasConstant.AZN;

import az.kapitalbank.marketplace.constant.AtlasConstant;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseCompleteRequest {
    BigDecimal amount;
    Long id;
    String pan;
    String rrn;
    String uid;
    String approvalCode;
    Integer installments;
    @Builder.Default
    private Integer currency = AZN;
    @Builder.Default
    private String terminalName = AtlasConstant.TERMINAL_NAME;
}
