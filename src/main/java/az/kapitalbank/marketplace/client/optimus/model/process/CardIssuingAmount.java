package az.kapitalbank.marketplace.client.optimus.model.process;

import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = PRIVATE)
public class CardIssuingAmount {
    BigDecimal issuingAmount;
    BigDecimal refinancingAmount;
}
