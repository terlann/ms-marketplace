package az.kapitalbank.marketplace.dto;

import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteScoring {
    UUID trackId;
    String taskId;
    String businessKey;
    String additionalNumber1;
    String additionalNumber2;
    CustomerDecision customerDecision;
}
