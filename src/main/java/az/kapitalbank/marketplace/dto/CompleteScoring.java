package az.kapitalbank.marketplace.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteScoring {
    String trackId;
    String businessKey;
    String additionalNumber1;
    String additionalNumber2;
}
