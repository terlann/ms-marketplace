package az.kapitalbank.marketplace.messaging.event;

import az.kapitalbank.marketplace.constant.FraudType;
import java.util.List;
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
    private String fraudResultStatus;
    private List<FraudType> types;
}
