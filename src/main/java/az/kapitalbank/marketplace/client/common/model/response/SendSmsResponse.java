package az.kapitalbank.marketplace.client.common.model.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SendSmsResponse {
    UUID id;
}
