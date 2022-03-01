package az.kapitalbank.marketplace.client.optimus.model.scoring;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartScoringRequest {
    String processKey;
    StartScoringVariable variables;
}
