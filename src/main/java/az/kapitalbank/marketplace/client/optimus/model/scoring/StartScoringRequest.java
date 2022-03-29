package az.kapitalbank.marketplace.client.optimus.model.scoring;

import az.kapitalbank.marketplace.constant.OptimusConstant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartScoringRequest {
    @Builder.Default
    String processKey = OptimusConstant.PROCESS_KEY;
    StartScoringVariable variables;
}
