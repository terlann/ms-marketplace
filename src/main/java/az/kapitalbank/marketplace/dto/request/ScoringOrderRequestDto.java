package az.kapitalbank.marketplace.dto.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import az.kapitalbank.marketplace.constants.ScoringStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoringOrderRequestDto {
    @NotEmpty
    String telesalesOrderId;
    @NotNull
    ScoringStatus scoringStatus;
    LocalDate loanContractStartDate;
    LocalDate loanContractEndDate;
    String cardPan;
}
