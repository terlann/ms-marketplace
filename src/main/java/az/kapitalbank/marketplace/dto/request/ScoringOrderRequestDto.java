package az.kapitalbank.marketplace.dto.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

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
    LocalDate loanStartDate;
    LocalDate loanEndDate;
    UUID cardUUID;
}
