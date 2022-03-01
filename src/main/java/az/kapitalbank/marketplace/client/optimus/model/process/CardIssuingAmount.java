package az.kapitalbank.marketplace.client.optimus.model.process;

import java.math.BigDecimal;

import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(level = PRIVATE)
public class CardIssuingAmount {
    BigDecimal issuingAmount;
    BigDecimal refinancingAmount;
}
