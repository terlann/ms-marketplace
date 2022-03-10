package az.kapitalbank.marketplace.client.optimus.model.process;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Salary {
    BigDecimal value;
}
