package az.kapitalbank.marketplace.client.optimus.model.process;

import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDate;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@FieldDefaults(level = PRIVATE)
public class CreateCardCreditRequest {
    LocalDate startDate;
    LocalDate endDate;
}
