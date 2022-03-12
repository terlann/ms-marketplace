package az.kapitalbank.marketplace.client.optimus.model.process;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class Offer {
    BigDecimal interestRate;
    @JsonAlias({"availableLoanAmount", "availableLoanLimit"})
    BigDecimal availableLoanAmount;
}
