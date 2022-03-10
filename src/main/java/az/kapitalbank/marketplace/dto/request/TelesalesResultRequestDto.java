package az.kapitalbank.marketplace.dto.request;

import az.kapitalbank.marketplace.constant.ScoringStatus;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelesalesResultRequestDto {
    @NotBlank
    String telesalesOrderId;
    @NotNull
    ScoringStatus scoringStatus;
    @NotNull
    LocalDate loanStartDate;
    @NotNull
    LocalDate loanEndDate;
    @NotBlank
    @Size(min = 16, max = 16)
    String pan;
}
