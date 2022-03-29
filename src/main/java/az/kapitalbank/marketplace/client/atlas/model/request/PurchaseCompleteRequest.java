package az.kapitalbank.marketplace.client.atlas.model.request;

import static az.kapitalbank.marketplace.constant.AtlasConstant.AZN;
import static az.kapitalbank.marketplace.constant.AtlasConstant.TERMINAL_NAME;

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
    Integer currency = AZN;
    @Builder.Default
    String terminalName = TERMINAL_NAME;
}
