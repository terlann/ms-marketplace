package az.kapitalbank.marketplace.client.optimus.model.scoring;

import static az.kapitalbank.marketplace.constant.OptimusConstant.PROCESS_KEY;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartScoringRequest {
    @Builder.Default
    String processKey = PROCESS_KEY;
    StartScoringVariable variables;
}
