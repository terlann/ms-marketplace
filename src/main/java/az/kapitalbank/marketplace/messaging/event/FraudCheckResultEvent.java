package az.kapitalbank.marketplace.messaging.event;

import java.util.UUID;

import az.kapitalbank.marketplace.constant.FraudResultStatus;
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
    UUID trackId;
    FraudResultStatus fraudResultStatus;
}
