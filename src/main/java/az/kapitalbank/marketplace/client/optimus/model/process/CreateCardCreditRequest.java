package az.kapitalbank.marketplace.client.optimus.model.process;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;


@Data
@FieldDefaults(level = PRIVATE)
public class CreateCardCreditRequest {
    LocalDate startDate;
    LocalDate endDate;
}
