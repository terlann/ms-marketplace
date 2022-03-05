package az.kapitalbank.marketplace.client.atlas.model.response;

import java.math.BigDecimal;

import az.kapitalbank.marketplace.constant.AccountStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    BigDecimal availableBalance;
    BigDecimal overdraftLimit;
    AccountStatus status;
}
