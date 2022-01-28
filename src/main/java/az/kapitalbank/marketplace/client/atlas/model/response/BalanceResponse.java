package az.kapitalbank.marketplace.client.atlas.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BalanceResponse {

    String accountNumber;
    String availableBalance;
    int currency;
    int overdraftLimit;

}
