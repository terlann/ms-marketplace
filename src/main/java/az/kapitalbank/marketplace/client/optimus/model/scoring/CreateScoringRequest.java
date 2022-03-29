package az.kapitalbank.marketplace.client.optimus.model.scoring;

import static az.kapitalbank.marketplace.constant.OptimusConstant.NAME_ON_CARD;
import static az.kapitalbank.marketplace.constant.OptimusConstant.SALES_SOURCE;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateScoringRequest {
    String cardDemandedAmount;
    @Builder.Default
    String salesSource = SALES_SOURCE;
    @Builder.Default
    String nameOnCard = NAME_ON_CARD;
    String cashDemandedAmount;
    boolean preApproval;
    CustomerDecision customerDecision;
}
