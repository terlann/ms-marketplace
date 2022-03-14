package az.kapitalbank.marketplace.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(staticName = "of")
public class CreateOrderResponse {
    private UUID trackId;
}


