package az.kapitalbank.marketplace.client.optimus.model.process;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Salary {
    BigDecimal value;
}
