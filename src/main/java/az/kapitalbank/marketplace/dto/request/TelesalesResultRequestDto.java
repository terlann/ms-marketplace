package az.kapitalbank.marketplace.dto.request;

import az.kapitalbank.marketplace.constant.ScoringStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelesalesResultRequestDto {
    @NotBlank
    String telesalesOrderId;
    @NotNull
    ScoringStatus scoringStatus;
    LocalDate loanContractStartDate;
    LocalDate loanContractEndDate;
    String uid;
    String pan;
    BigDecimal scoredAmount;
    Long rejectReasonCode;
    String cif;
    String contractNumber;
}
