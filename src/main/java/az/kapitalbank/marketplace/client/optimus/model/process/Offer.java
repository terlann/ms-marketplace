package az.kapitalbank.marketplace.client.optimus.model.process;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(level = PRIVATE)
public class Offer {
    BigDecimal interestRate;
    @JsonAlias({"availableLoanAmount", "availableLoanLimit"})
    BigDecimal availableLoanAmount;
}
