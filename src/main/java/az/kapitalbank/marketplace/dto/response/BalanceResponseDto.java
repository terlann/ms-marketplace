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
    @Builder.Default
    BigDecimal loanLimit = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal loanUtilized = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal availableBalance = BigDecimal.ZERO;
}
