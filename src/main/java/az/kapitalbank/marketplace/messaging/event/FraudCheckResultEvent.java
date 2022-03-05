package az.kapitalbank.marketplace.messaging.event;

import java.util.List;
import java.util.UUID;

import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.FraudType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResultEvent {
    private UUID trackId;
    private FraudResultStatus fraudResultStatus;
    private List<FraudType> types;
}
