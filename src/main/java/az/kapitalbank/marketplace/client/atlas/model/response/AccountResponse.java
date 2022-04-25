package az.kapitalbank.marketplace.client.atlas.model.response;

import az.kapitalbank.marketplace.constant.AccountStatus;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    BigDecimal availableBalance;
    BigDecimal overdraftLimit;
    AccountStatus status;
}