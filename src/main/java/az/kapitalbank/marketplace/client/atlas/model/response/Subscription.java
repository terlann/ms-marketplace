package az.kapitalbank.marketplace.client.atlas.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Subscription {
    private String channel;
    private String scheme;
    private String address;
}
