package az.kapitalbank.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceResponseDto {
    LocalDate loanEndDate;
    BigDecimal loanLimit;
    BigDecimal loanUtilized;
    BigDecimal loanAvailable;
}
