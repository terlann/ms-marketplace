package az.kapitalbank.marketplace.messaging.event;

import az.kapitalbank.marketplace.constant.FraudResultStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResultEvent {
    private UUID trackId;
    private FraudResultStatus fraudResultStatus;
}
