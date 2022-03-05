package az.kapitalbank.marketplace.client.umico.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UmicoDecisionRequest {
    UUID trackId;
    UmicoDecisionStatus decisionStatus;
    LocalDate loanContractStartDate;
    LocalDate loanContractEndDate;
    String dvsUrl;
    UUID customerId;
    BigDecimal loanLimit;
    Integer loanTerm;
    BigDecimal commission;
}
