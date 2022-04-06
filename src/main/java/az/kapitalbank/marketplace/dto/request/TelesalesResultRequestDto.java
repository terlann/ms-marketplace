package az.kapitalbank.marketplace.dto.request;

import az.kapitalbank.marketplace.constant.ScoringStatus;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
public class TelesalesResultRequestDto {
    @NotBlank
    String telesalesOrderId;
    @NotNull
    ScoringStatus scoringStatus;
    @NotNull
    LocalDate loanContractStartDate;
    @NotNull
    LocalDate loanContractEndDate;
    @NotBlank
    String uid;
    @NotBlank
    String pan;
}
