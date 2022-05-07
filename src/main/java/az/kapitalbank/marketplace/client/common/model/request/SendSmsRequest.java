package az.kapitalbank.marketplace.client.common.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SendSmsRequest {
    private String body;
    private String phoneNumber;
}
