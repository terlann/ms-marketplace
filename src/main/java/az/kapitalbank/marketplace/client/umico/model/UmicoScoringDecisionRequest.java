package az.kapitalbank.marketplace.client.umico.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UmicoScoringDecisionRequest {
    UUID trackId;
    String scoringStatus;
    LocalDate loanStartDate;
    LocalDate loanEndDate;
    String dvsUrl;
    UUID customerId;
    Integer loanLimit;
    Integer loanTerm;
    BigDecimal commission;
}
