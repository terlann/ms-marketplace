package az.kapitalbank.marketplace.dto.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import az.kapitalbank.marketplace.constants.OrderScoringStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ScoringOrderRequestDto {
    @NotEmpty
    String eteOrderId;
    @NotNull
    OrderScoringStatus scoringStatus;
    String creditId;
    LocalDate loanStartDate;
    LocalDate loanEndDate;
}
