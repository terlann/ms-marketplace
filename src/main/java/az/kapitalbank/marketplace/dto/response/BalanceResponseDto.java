package az.kapitalbank.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class BalanceResponseDto {
    LocalDateTime cardExpiryDate;
    BigDecimal loanLimit;
    BigDecimal loanUtilized;
    BigDecimal availableBalance;
}
