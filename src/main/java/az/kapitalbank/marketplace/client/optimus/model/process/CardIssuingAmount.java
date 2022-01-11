package az.kapitalbank.marketplace.client.optimus.model.process;

import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(level = PRIVATE)
public class CardIssuingAmount {
    BigDecimal issuingAmount;
    BigDecimal refinancingAmount;
}
