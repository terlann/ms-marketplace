package az.kapitalbank.marketplace.client.atlas.model.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BalanceResponse {

    String accountNumber;
    BigDecimal availableBalance;
    int currency;
    BigDecimal overdraftLimit;

}
