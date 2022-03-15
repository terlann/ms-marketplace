package az.kapitalbank.marketplace.client.optimus.model.process;

import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDate;
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
public class CreateCardCreditRequest {
    LocalDate startDate;
    LocalDate endDate;
}
