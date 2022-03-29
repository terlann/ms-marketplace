package az.kapitalbank.marketplace.client.optimus.model.scoring;

import static az.kapitalbank.marketplace.constant.OptimusConstant.CARD_PRODUCT_CODE;
import static az.kapitalbank.marketplace.constant.OptimusConstant.PROCESS_PRODUCT_TYPE;

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
    @Builder.Default
    String cardProductCode = CARD_PRODUCT_CODE;
    String salesSource;
    @Builder.Default
    String processProductType = PROCESS_PRODUCT_TYPE;
    @Builder.Default
    boolean scoreCard = true;
    boolean scoreCash;
    boolean preApproval;
    @Builder.Default
    boolean phoneNumberVerified = true;
    @Builder.Default
    boolean isMarketPlaceOperation = true;
    CustomerIdentification customerIdentification;
}
