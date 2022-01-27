package az.kapitalbank.marketplace.dto;

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
    String businessKey;
    String additionalNumber1;
    String additionalNumber2;
}
