package az.kapitalbank.marketplace.client.optimus.model.process;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProcessVariableResponse {
    String pan;
    String uid;
    String cif;
    String cardCreditContractNumber;
}
