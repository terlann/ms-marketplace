package az.kapitalbank.marketplace.messaging.event;

import az.kapitalbank.marketplace.constants.FraudResultStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FraudCheckResultEvent {
    String trackId;
    FraudResultStatus fraudResultStatus;
}
