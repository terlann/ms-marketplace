package az.kapitalbank.marketplace.client.optimus.model.scoring;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateScoringRequest {
    String cardDemandedAmount;
    String salesSource;
    String nameOnCard;
    String cashDemandedAmount;
    boolean preApproval;
    CustomerDecision customerDecision;
}
