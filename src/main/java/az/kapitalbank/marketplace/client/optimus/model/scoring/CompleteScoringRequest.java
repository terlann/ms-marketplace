package az.kapitalbank.marketplace.client.optimus.model.scoring;

import static az.kapitalbank.marketplace.constant.OptimusConstant.CENTRAL_BRANCH_CODE;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteScoringRequest {
    @Builder.Default
    String deliveryBranchCode = CENTRAL_BRANCH_CODE;
    CustomerDecision customerDecision;
    CustomerContact customerContact;
}
