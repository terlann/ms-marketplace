package az.kapitalbank.marketplace.client.atlas.model.request;

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
    Integer currency;
    String pan;
    String rrn;
    String uid;
    String terminalName;
    String approvalCode;
    Integer installments;
}
