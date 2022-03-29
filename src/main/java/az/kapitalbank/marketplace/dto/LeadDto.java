package az.kapitalbank.marketplace.dto;

import az.kapitalbank.marketplace.constant.FraudResultStatus;
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
public class LeadDto {
    private UUID trackId;
    private FraudResultStatus fraudResultStatus;
    private List<FraudType> types;

    public LeadDto(UUID trackId) {
        this.trackId = trackId;
    }
}
