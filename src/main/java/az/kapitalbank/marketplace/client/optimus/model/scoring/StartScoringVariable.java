package az.kapitalbank.marketplace.client.optimus.model.scoring;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartScoringVariable {
    String pin;
    String phoneNumber;
    String cardProductCode;
    String salesSource;
    String processProductType;
    boolean scoreCard;
    boolean scoreCash;
    boolean preApproval;
    boolean phoneNumberVerified;
    boolean isMarketPlaceOperation;
    CustomerIdentification customerIdentification;
}
